package com.joy.joymall.seckill.service;


import com.joy.joymall.seckill.to.SecKillSkuRedisTo;

import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/12 0:44
 */
public interface SecKillService {
    void uploadSecKillSkuLatestThreeDays();

    List<SecKillSkuRedisTo> getCurrentSecKillSuks();

    SecKillSkuRedisTo getSkuSecKillInfo(Long skuId);

    String kill(String killId, String key, Long num);
}
