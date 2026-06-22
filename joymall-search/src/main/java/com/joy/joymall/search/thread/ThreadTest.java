package com.joy.joymall.search.thread;

import java.util.concurrent.*;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/2 20:06
 */
public class ThreadTest {


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main....start....");

//        FutureTask<Integer> futureTask = new FutureTask<Integer>(new CallableTest());
//        new Thread(futureTask).start();
//        System.out.println("等待任务执行中...");
//        //到此就阻塞等待最后结果执行，但前面还是异步
//        System.out.println(futureTask.get());
        Future<Integer> submit = SelfThreadPool.executor.submit(new CallableTest());
        System.out.println(submit.get());
        System.out.println("main....end....");


    }


    public static class CallableTest implements Callable<Integer> {

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程号:" + Thread.currentThread().getId());
            int i = 10 / 2;
            return i;
        }
    }
}
