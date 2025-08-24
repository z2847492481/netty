package com.zhq.netty.bootstrap;

import com.zhq.netty.channel.nio.NioEventLoop;
import com.zhq.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @author zhq123
 * @date 2025/8/18
 **/
public class Bootstrap {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    private NioEventLoop nioEventLoop;

    private NioEventLoopGroup nioEventLoopGroup;

    private SocketChannel socketChannel;

    public Bootstrap() {

    }

    public Bootstrap setNioEventLoopGroup(NioEventLoopGroup nioEventLoopGroup) {
        this.nioEventLoopGroup = nioEventLoopGroup;
        return this;
    }

    public Bootstrap setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        return this;
    }

    public void connect(String inetHost, int inetPort) {
        connect(new InetSocketAddress(inetHost, inetPort));
    }

    public void connect(SocketAddress localAddress) {
        doConnect(localAddress);
    }

    private void doConnect(SocketAddress localAddress) {
        // 先从nio线程组中获取一个线程
        nioEventLoop = nioEventLoopGroup.next();
        // 设置SocketChannel 用于后续处理事件时判断
        nioEventLoop.setSocketChannel(socketChannel);
        // 注册SocketChannel
        nioEventLoop.register(socketChannel,this.nioEventLoop);
        // 连接服务端
        doConnect0(localAddress);
    }

    private void doConnect0(SocketAddress localAddress) {
        nioEventLoop.execute(() -> {
            try {
                socketChannel.connect(localAddress);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        });
    }
}
