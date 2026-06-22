package com.joy.joymall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.coupon.dao.SeckillSkuRelationDao;
import com.joy.joymall.coupon.entity.SeckillSkuRelationEntity;
import com.joy.joymall.coupon.service.SeckillSkuRelationService;


@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<SeckillSkuRelationEntity> lambda = new QueryWrapper<SeckillSkuRelationEntity>().lambda();
        String sessionId = (String) params.get("promotionSessionId");
        if (!StringUtils.isEmpty(sessionId)) {
            lambda.eq(SeckillSkuRelationEntity::getPromotionSessionId, sessionId);
        }

        IPage<SeckillSkuRelationEntity> page = this.page(
                new Query<SeckillSkuRelationEntity>().getPage(params),
                lambda
        );

        return new PageUtils(page);
    }

}