package com.zhq.netty.channel;

import com.zhq.netty.channel.nio.NioEventLoop;
import com.zhq.netty.util.concurrent.DefaultThreadFactory;
import com.zhq.netty.util.concurrent.SingleThreadEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

/**
 * 提供register方法
 * @author zhq123
 * @date 2025/8/17
 **/
public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventLoop.class);

    public SingleThreadEventLoop(){
        super();
    }

    protected SingleThreadEventLoop(Executor executor, EventLoopTaskQueueFactory queueFactory) {
        super(executor, queueFactory, new DefaultThreadFactory());
    }

    /**
     * 服务端调用
     * @param channel
     * @param nioEventLoop
     */
    public void register(ServerSocketChannel channel, NioEventLoop nioEventLoop) {
        if (inEventLoop(Thread.currentThread())) {
            register0(channel, nioEventLoop);
        } else {
            nioEventLoop.execute(() -> {
                register0(channel, nioEventLoop);
                logger.info("ServerSocketChannel已经注册到selector上，正在监听客户端的连接事件...");
            });
        }
    }

    /**
     * 客户端调用
     * @param channel
     * @param nioEventLoop
     */
    public void register(SocketChannel channel, NioEventLoop nioEventLoop) {
        // 如果执行该方法的线程就是执行器中的线程，直接执行方法即可
        if (inEventLoop(Thread.currentThread())) {
            register0(channel,nioEventLoop);
        }else {
            // 在这里，第一次向单线程执行器中提交任务的时候，执行器终于开始执行了
            nioEventLoop.execute(() -> {
                register0(channel,nioEventLoop);
                logger.info("客户端的channel已注册到多路复用器上了！:{}",Thread.currentThread().getName());
            });
        }
    }


    /**
     * 服务端对OP_ACCEPT事件感兴趣
     * @param channel
     * @param nioEventLoop
     */
    private void register0(ServerSocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.unwrappedSelector(), SelectionKey.OP_ACCEPT);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * 客户端对OP_CONNECT事件感兴趣
     * @param channel
     * @param nioEventLoop
     */
    private void register0(SocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.unwrappedSelector(), SelectionKey.OP_CONNECT);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void registerRead(SocketChannel channel, NioEventLoop nioEventLoop) {
        //如果执行该方法的线程就是执行器中的线程，直接执行方法即可
        if (inEventLoop(Thread.currentThread())) {
            register0(channel, nioEventLoop);
        } else {
            //在这里，第一次向单线程执行器中提交任务的时候，执行器终于开始执行了
            nioEventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    register00(channel, nioEventLoop);
                    logger.info("客户端的channel已注册到多路复用器上了！:{}", Thread.currentThread().getName());
                }
            });
        }
    }

    private void register00(SocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.unwrappedSelector(), SelectionKey.OP_READ);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
