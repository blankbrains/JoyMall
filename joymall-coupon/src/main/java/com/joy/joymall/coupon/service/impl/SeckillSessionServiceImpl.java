package com.joy.joymall.coupon.service.impl;

import com.joy.joymall.coupon.entity.SeckillSkuRelationEntity;
import com.joy.joymall.coupon.service.SeckillSkuRelationService;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.coupon.dao.SeckillSessionDao;
import com.joy.joymall.coupon.entity.SeckillSessionEntity;
import com.joy.joymall.coupon.service.SeckillSessionService;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Resource
    private SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionEntity> getLatestThreeDaysSecKillSku() {
        //因为定时任务是每天晚上凌晨3点，应该减去3小时，然后再加上三天，得到这个时间段要上架的商品
        List<SeckillSessionEntity> seckillSessionEntities = this.list(new QueryWrapper<SeckillSessionEntity>()
                .lambda()
                .between(SeckillSessionEntity::getStartTime, startTime(), endTime()));

        //封装当前秒杀需要的商品，循环查库，可以改成查询出所有，然后按PromotionSessionId逐个封装，就这样吧
        List<SeckillSessionEntity> seckillSessionEntityList = null;
        if (!CollectionUtils.isEmpty(seckillSessionEntities)) {
            seckillSessionEntityList = seckillSessionEntities.stream().peek((entity) -> {
                Long currentSecKillId = entity.getId();
                List<SeckillSkuRelationEntity> secKillSkus = seckillSkuRelationService
                        .list(new QueryWrapper<SeckillSkuRelationEntity>()
                                .lambda()
                                .eq(SeckillSkuRelationEntity::getPromotionSessionId, currentSecKillId));
                entity.setRelationSkus(secKillSkus);
            }).collect(Collectors.toList());
        }
        return seckillSessionEntityList;
    }


    /**
     * @return ： 返回三天内的起始时间
     */
    private String startTime() {
        LocalDate now = LocalDate.now();
        LocalTime min = LocalTime.MIN;
        LocalDateTime startTime = LocalDateTime.of(now, min);

        return startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


    /**
     * @return : 返回三天内的结束时间
     */
    private String endTime() {
        LocalDate afterThreeDays = LocalDate.now().plusDays(2L);
        LocalTime max = LocalTime.MAX;
        LocalDateTime endTime = LocalDateTime.of(afterThreeDays, max);
        return endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

}