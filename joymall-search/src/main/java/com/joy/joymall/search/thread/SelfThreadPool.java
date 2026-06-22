package com.joy.joymall.search.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/2 21:42
 */
public class SelfThreadPool {

    public static ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 200,
            1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100),
            Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
}
