package com.zhq.netty.channel;

import java.util.Queue;

/**
 * @author zhq123
 * @date 2025/8/18
 **/
public interface EventLoopTaskQueueFactory {

    Queue<Runnable> newTaskQueue(int maxCapacity);
}
