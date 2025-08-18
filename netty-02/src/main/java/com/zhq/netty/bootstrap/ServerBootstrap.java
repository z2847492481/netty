package com.zhq.netty.bootstrap;

import com.zhq.netty.channel.nio.NioEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;

/**
 * @author zhq123
 * @date 2025/8/17
 **/
public class ServerBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(ServerBootstrap.class);

    private NioEventLoop nioEventLoop;

    private ServerSocketChannel serverSocketChannel;

    public ServerBootstrap() {

    }

    /**
     * 便于链式调用，下同
     * @param nioEventLoop
     * @return
     */
    public ServerBootstrap setNioEventLoop(NioEventLoop nioEventLoop) {
        this.nioEventLoop = nioEventLoop;
        return this;
    }

    public ServerBootstrap setServerSocketChannel(ServerSocketChannel serverSocketChannel) {
        this.serverSocketChannel = serverSocketChannel;
        return this;
    }

    public void bind(String host,int inetPort) {
        bind(new InetSocketAddress(host,inetPort));
    }

    public void bind(SocketAddress localAddress) {
        doBind(localAddress);
    }

    /**
     * 虽然两个操作都是异步的，但是任务有先后，只要保证启动的时候不会反复启动即可
     * @param localAddress
     */
    private void doBind(SocketAddress localAddress) {
        // 注册selector
        nioEventLoop.register(serverSocketChannel,this.nioEventLoop);
        // 启动服务器
        doBind0(localAddress);
    }

    private void doBind0(SocketAddress localAddress) {
        nioEventLoop.execute(() -> {
            try {
                serverSocketChannel.bind(localAddress);
                logger.info("服务端channel和端口号绑定了");
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        });
    }
}
