package com.zhq.netty.channel.nio;

import java.util.Random;

/**
 * @author zhq123
 * @date 2025/8/24
 **/
public class NioEventLoopGroup {

    Random random = new Random();

    private final NioEventLoop[] nioEventLoops;

    /**
     * 简单起见，只向外提供一个简单的构造方法
     * @param nThreads 线程数量
     */
    public NioEventLoopGroup(int nThreads) {
        nioEventLoops = new NioEventLoop[nThreads];
        for (int i = 0; i < nThreads; i++) {
            nioEventLoops[i] = new NioEventLoop();
        }
    }

    public NioEventLoop next() {
        return nioEventLoops[random.nextInt(nioEventLoops.length)];
    }
}
