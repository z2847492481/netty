package com.zhq.netty.channel.nio;

import com.zhq.netty.channel.EventLoopTaskQueueFactory;
import com.zhq.netty.channel.SingleThreadEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * @author zhq123
 * @date 2025/8/17
 **/
public class NioEventLoop extends SingleThreadEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(NioEventLoop.class);

    private ServerSocketChannel serverSocketChannel;

    private SocketChannel socketChannel;

    private NioEventLoop worker;

    private Selector selector;

    private SelectorProvider provider;

    public NioEventLoop() {
        super();
    }

    public NioEventLoop(ServerSocketChannel serverSocketChannel, SocketChannel socketChannel) {
        this(null, SelectorProvider.provider(), null, serverSocketChannel, socketChannel);
    }

    public NioEventLoop(Executor executor, SelectorProvider selectorProvider, EventLoopTaskQueueFactory queueFactory,
                        ServerSocketChannel serverSocketChannel, SocketChannel socketChannel) {
        super(executor, queueFactory);
        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        if (serverSocketChannel != null && socketChannel != null) {
            throw new RuntimeException("only one channel can be here! server or client!");
        }
        this.provider = selectorProvider;
        this.serverSocketChannel = serverSocketChannel;
        this.socketChannel = socketChannel;
        this.selector = openSelector();
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void setServerSocketChannel(ServerSocketChannel serverSocketChannel) {
        this.serverSocketChannel = serverSocketChannel;
    }

    public void setWorker(NioEventLoop worker) {
        this.worker = worker;
    }

    private Selector openSelector() {
        //未包装过的选择器
        final Selector unwrappedSelector;
        try {
            unwrappedSelector = provider.openSelector();
            return unwrappedSelector;
        } catch (IOException e) {
            throw new RuntimeException("failed to open a new selector", e);
        }
    }

    public Selector unwrappedSelector() {
        return selector;
    }

    private void select() throws IOException {
        Selector selector = this.selector;
        //这里是一个死循环
        for (;;){
            //如果没有就绪事件，就在这里阻塞3秒
            int selectedKeys = selector.select(3000);
            //如果有事件或者单线程执行器中有任务待执行，就退出循环
            if (selectedKeys != 0 || hasTasks()) {
                break;
            }
        }
    }

    private void processSelectedKey(SelectionKey k) throws Exception {
        if (socketChannel != null) {
            if (k.isConnectable()) {
                //channel已经连接成功
                if (socketChannel.finishConnect()) {
                    //注册读事件
                    socketChannel.register(selector,SelectionKey.OP_READ);
                    logger.info("已成功连接!");
                }
            }
            //如果是读事件
            if (k.isReadable()) {
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                socketChannel.read(byteBuffer);
                byteBuffer.flip();
                logger.info("读到的数据：{}", StandardCharsets.UTF_8.decode(byteBuffer));
            }
            return;
        }
        if (serverSocketChannel != null) {
            //连接事件
            if (k.isAcceptable()) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                //由worker执行器去执行注册
                worker.registerRead(socketChannel,worker);
                socketChannel.write(ByteBuffer.wrap("我还不是netty，但我知道你上线了".getBytes(StandardCharsets.UTF_8)));
                logger.info("服务器发送消息成功！");
            }
            if (k.isReadable()) {
                // 同样有两种方式得到客户端的channel，这里只列出一种
                SocketChannel channel = (SocketChannel)k.channel();
                // 分配字节缓冲区来接受客户端传过来的数据
                // 数据超过1024字节，下面的这种读取方式就会有问题
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                // 向buffer写入客户端传来的数据
                channel.read(buffer);
                //切换buffer的读模式
                buffer.flip();
                logger.info("读取到客户端数据：{}", StandardCharsets.UTF_8.decode(buffer));
            }
        }
    }

    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) throws Exception {
        if (selectedKeys.isEmpty()) {
            return;
        }
        Iterator<SelectionKey> i = selectedKeys.iterator();
        for (;;) {
            final SelectionKey k = i.next();
            // 每处理完一个就要remove
            i.remove();
            // really process the io event
            processSelectedKey(k);
            if (!i.hasNext()) {
                break;
            }
        }
    }

    private void processSelectedKeys() throws Exception {
        processSelectedKeysPlain(selector.selectedKeys());
    }

    @Override
    protected void run() {
        for (;;) {
            try {
                // 监听网络事件
                select();
                // 处理网络事件
                processSelectedKeys();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 处理完了网络事件或者没有网络事件之后，就可以来处理普通任务了
                runAllTasks();
            }
        }
    }

}
