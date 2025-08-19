package com.zhq.netty.test;

import com.zhq.netty.bootstrap.Bootstrap;
import com.zhq.netty.channel.nio.NioEventLoop;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author zhq123
 * @date 2025/8/19
 **/
public class TestClient {
    public static void main(String[] args) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.setNioEventLoop(new NioEventLoop(null,socketChannel)).
                setSocketChannel(socketChannel);
        bootstrap.connect("127.0.0.1",8081);
    }
}
