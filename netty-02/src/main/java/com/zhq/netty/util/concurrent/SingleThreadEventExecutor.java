package com.zhq.netty.util.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * @author zhq123
 * @date 2025/8/17
 **/
public abstract class SingleThreadEventExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventExecutor.class);

    private volatile Thread thread;

    private Queue<Runnable> taskQueue;

    /**
     * 默认最大任务数
     */
    private static final int DEFAULT_MAX_PENDING_TASKS = Integer.MAX_VALUE;

    final boolean offerTask(Runnable task) {
        return taskQueue.offer(task);
    }

    private RejectedExecutionHandler rejectedExecutionHandler;

    protected final void reject(Runnable task) {
        rejectedExecutionHandler.rejected(task, this);
    }

    private void addTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        //如果添加失败，执行拒绝策略
        if (!offerTask(task)) {
            reject(task);
        }
    }

    /**
     * 如accept，read，write，close这些操作都是eventLoop线程的负责的
     * 所以这个方法用来判断当前线程是不是eventLoop线程
     * @return
     */
    public boolean isEventLoopThread() {
        return thread == Thread.currentThread();
    }

    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        addTask(task);
        startThread();
    }

    private void startThread() {

    }


}
