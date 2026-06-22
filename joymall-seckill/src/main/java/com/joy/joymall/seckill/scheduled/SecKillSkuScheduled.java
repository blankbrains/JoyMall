package com.joy.joymall.seckill.scheduled;

import com.joy.joymall.seckill.service.SecKillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;


/**
 * @Description: 每天晚上三点，上架最近三天需要秒杀的商品
 * <p>
 * 当天：00:00:00-23:59:59秒，明天和后天也是一样
 * @Author:joymall
 * @Date:2022/9/12 0:39
 */
@Component
@Slf4j
public class SecKillSkuScheduled {

    @Resource
    private SecKillService secKillService;

    @Resource
    private RedissonClient redissonClient;

    private static final String UPLOAD_LOCK = "seckill:upload:lock";

    /**
     * TODO 幂等性处理：重复上架商品
     */
    @Scheduled(cron = "${seckill.cron}")
    public void uploadSecKillSkuLatestThreeDays() {
        //重复上架无需处理
        //使用redisson解决分布式锁
        RLock lock = redissonClient.getLock(UPLOAD_LOCK);
        try {
            lock.lock(10, TimeUnit.SECONDS );
            secKillService.uploadSecKillSkuLatestThreeDays();
        } finally {
            lock.unlock();
        }
    }

}
