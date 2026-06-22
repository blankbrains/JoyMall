//package com.joy.joymall.seckill.scheduled;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.*;
//import org.springframework.stereotype.Component;
//
//
//import java.util.Date;
//
///**
// * @Description:
// * @Author:joymall
// * @Date:2022/9/12 0:06
// */
//@EnableScheduling
//@Component
//@Slf4j
//@EnableAsync
//public class HelloScheduled {
//
//    /**
//     * spring cron表达式不允许有年
//     * 1-7代表周一-周日
//     * 定时任务默认阻塞，可以用spring的异步任务@EnableAsync，或者自己编写异步编排
//     *
//     * @throws : InterruptedException
//     */
//    @Async
//    @Scheduled(cron = "* * * * * *")
//    public void hello() throws InterruptedException {
//        log.info("当前是：{}", new Date().toString());
//        Thread.sleep(3000);
//    }
//}
