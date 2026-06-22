package com.joy.joymall.seckill.service.impl;


import cn.hutool.core.lang.UUID;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.joy.common.to.mq.SecKillOrderTo;
import com.joy.common.utils.R;
import com.joy.common.vo.MemberResponseVo;
import com.joy.joymall.seckill.feign.CouponFeignService;
import com.joy.joymall.seckill.feign.ProductFeignService;
import com.joy.joymall.seckill.interceptor.LoginUserInterceptor;
import com.joy.joymall.seckill.service.SecKillService;
import com.joy.joymall.seckill.to.SecKillSkuRedisTo;
import com.joy.joymall.seckill.vo.SecKillSessionsWithSkusVo;
import com.joy.joymall.seckill.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/12 0:45
 */
@Service
@Slf4j
public class SecKillServiceImpl implements SecKillService {

    @Resource
    private CouponFeignService couponFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private ProductFeignService productFeignService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RabbitTemplate rabbitTemplate;


    public static final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    public static final String SKU_CACHE_PREFIX = "seckill:skus";

    public static final String SKU_SEMAPHORE_PREFIX = "seckill:stock:";

    @Override
    public void uploadSecKillSkuLatestThreeDays() {
        //1.扫描最近三天需要秒杀的活动并封装商品
        R r = couponFeignService.getLatestThreeDaysSecKillSku();
        if (r.getCode() == 0) {
            //2.上架商品
            List<SecKillSessionsWithSkusVo> secKillSkus = r.getData(new TypeReference<List<SecKillSessionsWithSkusVo>>() {
            });

            if (!CollectionUtils.isEmpty(secKillSkus)) {
                //3.将上架商品缓存到redis
                //1).缓存活动信息
                cacheSessionInfos(secKillSkus);
                //2).缓存活动的关联商品信息
                cacheSessionSkuInfos(secKillSkus);
            }
        }
    }


    public List<SecKillSkuRedisTo> blockHandler(){
        log.error("getCurrentSecKillSuks方法被限流了。。。");
        return null;
    }
    /**
     * 获取当前参与秒杀的商品信息并在页面展示
     *
     * @return :
     */
    @SentinelResource(value = "getCurrentSecKillSuks",blockHandler = "blockHandler")
    @Override
    public List<SecKillSkuRedisTo> getCurrentSecKillSuks() {
        try (Entry entry = SphU.entry("seckillSkus")) {
            //1.确定当前时间属于那个秒杀场次
            long curTime = System.currentTimeMillis();
            Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
            //无秒杀场次
            if (CollectionUtils.isEmpty(keys)) {
                return null;
            }
            for (String key : keys) {
                String[] gapTime = key.replace(SESSION_CACHE_PREFIX, "").split("_");
                long startTime = Long.parseLong(gapTime[0]);
                long endTime = Long.parseLong(gapTime[1]);
                if (curTime >= startTime && curTime <= endTime) {
                    //2.获取当前秒杀场次的所有商品
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    //秒杀场次的value值是List，就是存放sku的key
                    BoundHashOperations<String, String, Object> hashOps = redisTemplate.boundHashOps(SKU_CACHE_PREFIX);
                    assert range != null;
                    List<Object> skus = hashOps.multiGet(range);
                    if (CollectionUtils.isEmpty(skus)) {
                        return null;
                    }
                    return skus.stream()
                            .map((skuJson) -> JSON.parseObject(skuJson.toString(), SecKillSkuRedisTo.class))
                            .collect(Collectors.toList());
                }
            }
        } catch (BlockException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public SecKillSkuRedisTo getSkuSecKillInfo(Long skuId) {
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKU_CACHE_PREFIX);

        Set<String> keys = hashOps.keys();
        if (!CollectionUtils.isEmpty(keys)) {
            for (String key : keys) {
                String seckillSkuId = key.split("_")[1];
                if (seckillSkuId.equals(skuId.toString())) {
                    String skuInfo = hashOps.get(key);
                    SecKillSkuRedisTo secKillSkuRedisTo = JSON.parseObject(skuInfo, SecKillSkuRedisTo.class);
                    Long startTime = secKillSkuRedisTo.getStartTime();
                    Long endTime = secKillSkuRedisTo.getEndTime();
                    long curTime = System.currentTimeMillis();
                    if (curTime >= startTime && curTime <= endTime) {
                        //在秒杀时间段
                    } else {
                        secKillSkuRedisTo.setRandomCode(null);
                    }
                    return secKillSkuRedisTo;
                }
            }
        }
        return null;
    }

    /**
     * @param killId sessionId_skuId  某个场次的某个商品
     * @param key    ： 随机码
     * @param num    ： 秒杀数量
     * @return ： 订单号
     */
    @Override
    public String kill(String killId, String key, Long num) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginThreadLocal.get();
        HashOperations<String, String, String> forHash = redisTemplate.opsForHash();
        String skuJson = forHash.get(SKU_CACHE_PREFIX, killId);
        if (StringUtils.isEmpty(skuJson)) {
            //商品秒杀完了
            return null;
        }
        SecKillSkuRedisTo skuRedisTo = JSON.parseObject(skuJson, SecKillSkuRedisTo.class);
        //校验时间合法性
        long endTime = skuRedisTo.getEndTime();
        long curTime = System.currentTimeMillis();

        //秒杀时间结束
        if (curTime > endTime) {
            return null;
        }
        //校验随机码和商品id
        //随机码正确：可能恶意脚本
        String token = skuRedisTo.getRandomCode();
        if (!token.equals(key)) {
            return null;
        }
        //商品不正确
        String skuId = skuRedisTo.getPromotionSessionId() + "_" + skuRedisTo.getSkuId();
        if (!skuId.equals(killId)) {
            return null;
        }
        //购买数量不合法
        if (num > skuRedisTo.getSeckillLimit().longValue()) {
            return null;
        }
        //用户购买了就加个key来确保只能购买一次，key=userId_sessionId_skuId
        String userPurchaseOrElseKey = memberResponseVo.getId() + "_" + skuRedisTo.getPromotionSessionId() + "_" + skuRedisTo.getSkuId();
        //设置过期时间为当前时间与场次结束时间的差值
        long ttl = endTime - curTime;
        //setIfAbsent==setNX,是原子性的
        Boolean userPurchaseOrElse = redisTemplate.opsForValue().setIfAbsent(userPurchaseOrElseKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
        if (userPurchaseOrElse != null && !userPurchaseOrElse) {
            return null;
        }
        //扣减信号量
        RSemaphore semaphore = redissonClient.getSemaphore(SKU_SEMAPHORE_PREFIX + token);
        try {
            //扣减信号量失败，秒杀库存不足
            boolean secKillSuccessOrElse = semaphore.tryAcquire(Math.toIntExact(num), 50L, TimeUnit.MILLISECONDS);
            if (!secKillSuccessOrElse) {
                return null;
            }
            //秒杀成功，创建订单号，并发消息给MQ,异步创建订单
            String orderSn = IdWorker.getTimeId();
            //发送消息给MQ,订单服务异步处理
            SecKillOrderTo secKillOrderTo = new SecKillOrderTo();
            secKillOrderTo.setOrderSn(orderSn);
            secKillOrderTo.setNum(num.intValue());
            secKillOrderTo.setSeckillPrice(skuRedisTo.getSeckillPrice());
            secKillOrderTo.setMemberId(memberResponseVo.getId());
            secKillOrderTo.setSkuId(skuRedisTo.getSkuId());
            secKillOrderTo.setPromotionSessionId(skuRedisTo.getPromotionSessionId());
            try {
                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", secKillOrderTo);
            } catch (Exception e) {
                log.error("秒杀订单MQ发送失败，回滚信号量: orderSn={}", orderSn, e);
                // MQ 发送失败时释放信号量，避免库存丢失
                semaphore.release(Math.toIntExact(num));
                return null;
            }
            return orderSn;
        } catch (InterruptedException e) {
            return null;
        }
    }


    /**
     * 保存要秒杀的信息，key为startTime_endTime value=skuIds
     *
     * @param secKillSkus :
     */
    private void cacheSessionInfos(List<SecKillSessionsWithSkusVo> secKillSkus) {
        secKillSkus.forEach((session) -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();

            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;
            List<String> skuIds = session.getRelationSkus()
                    .stream()
                    .map((item) -> {
                        return item.getPromotionSessionId() + "_" + item.getSkuId().toString();
                    })
                    .collect(Collectors.toList());
            //如果已经上架就不用再上架了
            Boolean hasKey = redisTemplate.hasKey(key);
            if (hasKey != null && !hasKey) {
                redisTemplate.opsForList().leftPushAll(key, skuIds);
            }
        });
    }


    /**
     * 缓存要秒杀的商品信息
     * 缓存的key为当前skuId，缓存的value为商品的秒杀信息+原信息+随机码
     *
     * @param secKillSkus ：
     */
    private void cacheSessionSkuInfos(List<SecKillSessionsWithSkusVo> secKillSkus) {
        //先获取hash绑定
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SKU_CACHE_PREFIX);
        //缓存商品信息到hash
        secKillSkus.forEach((session) -> {
            session.getRelationSkus().forEach((skus) -> {
                //随机码
                String token = UUID.randomUUID().toString().replace("-", "");
                Boolean hasSkuKey = hashOps.hasKey(skus.getPromotionSessionId() + "_" + skus.getSkuId().toString());
                if (hasSkuKey != null && !hasSkuKey) {
                    //缓存成JSON
                    SecKillSkuRedisTo secKillSkuRedisTo = new SecKillSkuRedisTo();
                    //1.sku基本信息（原价格）
                    //TODO 又是远程循环查询，效率低的雅痞
                    R r = productFeignService.getSkuInfo(skus.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo skuInfo = r.getData(new TypeReference<SkuInfoVo>() {
                        }, "skuInfo");
                        secKillSkuRedisTo.setSkuInfoVo(skuInfo);
                    }
                    //2.sku秒杀信息（秒杀价格等）
                    BeanUtils.copyProperties(skus, secKillSkuRedisTo);
                    //3.设置当前商品秒杀时间
                    secKillSkuRedisTo.setStartTime(session.getStartTime().getTime());
                    secKillSkuRedisTo.setEndTime(session.getEndTime().getTime());
                    //4.设置商品随机码：防止商品未开始秒杀就被恶意秒杀攻击，相当于一个token
                    secKillSkuRedisTo.setRandomCode(token);
                    //5.引入分布式信号量
                    //因为场次不同但是上架的商品可能相同，如果当前场次该商品已经上架，就不需要再次设置信号量
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_SEMAPHORE_PREFIX + token);
                    //信号量就是库存件数
                    semaphore.trySetPermits(skus.getSeckillCount().intValue());
                    //保存到Redis
                    String cacheSkuInfo = JSON.toJSONString(secKillSkuRedisTo);
                    //使用场次号_商品号作为key，主要是因为同一件商品在三天内可能在不同场次都会上架
                    hashOps.put(skus.getPromotionSessionId().toString() + "_" + skus.getSkuId().toString(), cacheSkuInfo);
                }
            });
        });
    }
}
