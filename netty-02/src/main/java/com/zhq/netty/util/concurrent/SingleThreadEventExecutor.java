package com.zhq.netty.util.concurrent;

import com.zhq.netty.channel.EventLoopTaskQueueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

/**
 * @author zhq123
 * @date 2025/8/17
 **/
public abstract class SingleThreadEventExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventExecutor.class);

    private volatile Thread thread;

    /**
     * 最底层干活的线程
     */
    private Executor executor;

    private Queue<Runnable> taskQueue;

    /**
     * 默认最大任务数
     */
    private static final int DEFAULT_MAX_PENDING_TASKS = Integer.MAX_VALUE;

    /**
     * 线程状态，0表示未启动，1表示已启动
     */
    private volatile int state = 0;

    protected SingleThreadEventExecutor(Executor executor, EventLoopTaskQueueFactory queueFactory, ThreadFactory threadFactory) {
        this(executor,queueFactory,threadFactory,RejectedExecutionHandlers.reject());
    }

    protected SingleThreadEventExecutor(Executor executor,EventLoopTaskQueueFactory queueFactory,ThreadFactory threadFactory,RejectedExecutionHandler rejectedExecutionHandler) {
        if (executor == null) {
            this.executor = new ThreadPerTaskExecutor(threadFactory);
        }
        this.taskQueue = queueFactory == null? newTaskQueue(DEFAULT_MAX_PENDING_TASKS):queueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        return new LinkedBlockingQueue<Runnable>(maxPendingTasks);
    }

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
     *
     * @return
     */
    public boolean inEventLoop(Thread thread) {
        return thread == this.thread;
    }

    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        addTask(task);
        startThread();
    }

    protected abstract void run();


    private void doStartThread() {
        executor.execute(() -> {
            // 保存EventLoop线程
            thread = Thread.currentThread();
            // 下面这段代码等同于运行子类的run方法
            SingleThreadEventExecutor.this.run();
        });
    }

    /**
     * 同步方法，保证线程启动一次
     */
    private synchronized void startThread() {
        if (state == 0) {
            doStartThread();
        }
    }

    protected boolean hasTasks() {
        return !taskQueue.isEmpty();
    }

    protected void runAllTasks() {
        runAllTasksFrom(taskQueue);
    }

    protected void runAllTasksFrom(Queue<Runnable> taskQueue) {
        //从任务对立中拉取任务,如果第一次拉取就为null，说明任务队列中没有任务，直接返回即可
        Runnable task = pollTaskFrom(taskQueue);
        if (task == null) {
            return;
        }
        for (;;) {
            //执行任务队列中的任务
            safeExecute(task);
            //执行完毕之后，拉取下一个任务，如果为null就直接返回
            task = pollTaskFrom(taskQueue);
            if (task == null) {
                return;
            }
        }
    }

    private void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            logger.warn("A task raised an exception. Task: {}", task, t);
        }
    }

    protected static Runnable pollTaskFrom(Queue<Runnable> taskQueue) {
        return taskQueue.poll();
    }

}
