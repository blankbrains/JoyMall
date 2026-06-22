package com.joy.joymall.ware.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.ware.dao.WareOrderTaskDetailDao;
import com.joy.joymall.ware.entity.WareOrderTaskDetailEntity;
import com.joy.joymall.ware.service.WareOrderTaskDetailService;


@Service("wareOrderTaskDetailService")
public class WareOrderTaskDetailServiceImpl extends ServiceImpl<WareOrderTaskDetailDao, WareOrderTaskDetailEntity> implements WareOrderTaskDetailService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareOrderTaskDetailEntity> page = this.page(
                new Query<WareOrderTaskDetailEntity>().getPage(params),
                new QueryWrapper<WareOrderTaskDetailEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<WareOrderTaskDetailEntity> getByOrderTaskIdConditionOfUnLocked(Long orderTaskEntityId) {
        return this.baseMapper.selectList(
                new QueryWrapper<WareOrderTaskDetailEntity>()
                        .lambda()
                        .eq(WareOrderTaskDetailEntity::getTaskId, orderTaskEntityId)
                        .eq(WareOrderTaskDetailEntity::getLockStatus, 1));
    }

}