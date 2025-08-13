package com.zhq.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * 1、为什么服务端也需要多路复用？
 *   客户端也可以创建多个channel连接多个服务器来操作不同的功能。
 * 2、为什么不需要显式的监听可写事件？
 *
 * @author zhq123
 * @date 2025/8/12
 **/
public class SimpleClient {

    private static Logger logger = LoggerFactory.getLogger(SimpleClient.class);

    public static void main(String[] args) throws IOException {
        // 打开一个客户端channel
        SocketChannel socketChannel = SocketChannel.open();
        // 设置为非阻塞
        socketChannel.configureBlocking(false);
        // 打开一个selector
        Selector selector = Selector.open();
        // 把channel注册到selector上，返回一个SelectionKey
        // SelectionKey包装了Channel、Channel感兴趣的事件、Channel就绪的事件
        SelectionKey selectionKey = socketChannel.register(selector, 0);
        // 设置感兴趣的事件(直译方法名)
        selectionKey.interestOps(SelectionKey.OP_CONNECT);
        // 连接服务端
        socketChannel.connect(new InetSocketAddress(8080));
        // 死循环
        while(true) {
            // select监听
            selector.select();
            // 获取所有SelectionKey，这里会返回感兴趣的事件触发了的SelectionKey的集合
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                // 处理完一个key之后要从selector的就绪列表中移除，否则会造成无限消费
                iterator.remove();
                // 处理可连接事件
                if (key.isConnectable()) {
                    // 完成连接
                    if (socketChannel.finishConnect()) {
                        // 连接上服务器之后，就要把感兴趣的事改成可读事件 此时register返回的还是还是上面那个SelectionKey
                        // 所以同一个selector对同一个channel只会生成一个SelectionKey
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        logger.info("已经注册了读事件!");
                        // 给服务端发个消息
                        socketChannel.write(ByteBuffer.wrap("客户端连接上了!".getBytes(StandardCharsets.UTF_8)));
                    }
                }
                // 可读事件
                if (key.isReadable()) {
                    SocketChannel channel = (SocketChannel)key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    channel.read(buffer);
                    buffer.flip();
                    logger.info("读到来自服务端的数据：{}", StandardCharsets.UTF_8.decode(buffer));
                }
            }
        }
    }
}
