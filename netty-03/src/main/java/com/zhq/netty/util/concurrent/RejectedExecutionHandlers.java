package com.zhq.netty.util.concurrent;

import java.util.concurrent.RejectedExecutionException;

/**
 * @author zhq123
 * @date 2025/8/18
 **/
public class RejectedExecutionHandlers {

    private static final RejectedExecutionHandler REJECT = (task, executor) -> {
        throw new RejectedExecutionException();
    };

    private RejectedExecutionHandlers() {
    }


    public static RejectedExecutionHandler reject() {
        return REJECT;
    }

}
