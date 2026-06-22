package com.joy.joymall.search.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/2 21:40
 */
public class CompletableFutureTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main...start...");
//        CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
//            System.out.println("runAsync:这样就执行了吗？");
//        }, SelfThreadPool.executor);

        /**
         * 方法完成后的感知
         */
//        CompletableFuture<String> supplyAsync = CompletableFuture.supplyAsync(() -> {
//            System.out.println("supplyAsync:" + Thread.currentThread().getId());
//            return "还得要返回值。。。";
//        }, SelfThreadPool.executor).whenCompleteAsync((result, e) -> {
//            if (e != null) {
//                System.out.println("异常：" + e);
//            }
//            System.out.println("线程完成后的返回值：" + result);
//        }, SelfThreadPool.executor).exceptionally((throwable -> {
//            return "";
//        }));
//        System.out.println("main...end...");


        /**
         * 方法完成后的处理
         */
//        CompletableFuture.supplyAsync(() -> {
//            System.out.println("supplyAsync:" + Thread.currentThread().getId());
//            return "还得要返回值。。。";
//        }, SelfThreadPool.executor).handle((result, exception) -> {
//            if (result != null) {
//                return result + "successfully";
//            }
//            if (exception != null) {
//                return exception + "";
//            }
//            return "nani";
//        }).thenApplyAsync((result) -> {
//            return result + "继续执行的任务";
//        }, SelfThreadPool.executor);


        /**
         * 双任务处理
         *
         */
//        CompletableFuture<String> futureTwo = CompletableFuture.supplyAsync(() -> {
//            System.out.println("supplyAsyncTwo" + Thread.currentThread().getId());
//            return "第二个异步任务";
//        }, SelfThreadPool.executor);
//
//        CompletableFuture<String> futureOne = CompletableFuture.supplyAsync(() -> {
//            System.out.println("supplyAsyncOne:" + Thread.currentThread().getId());
//            return "第一个异步任务。。。";
//        }, SelfThreadPool.executor).thenCombine(futureTwo, (one, two) -> {
//            System.out.println("两个任务处理完成后的结果。。。");
//            return one + two;
//        });

        /**
         * 多任务组合
         */
        CompletableFuture<Void> future = CompletableFuture.allOf();
    }
}
