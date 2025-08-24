package com.zhq.netty.util.concurrent;

/**
 * @author zhq123
 * @date 2025/8/17
 **/
public interface RejectedExecutionHandler {

    void rejected(Runnable task, SingleThreadEventExecutor executor);
}
