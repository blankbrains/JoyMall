package com.joy.joymall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.joy.common.utils.PageUtils;
import com.joy.joymall.ware.entity.WareInfoEntity;
import com.joy.joymall.ware.vo.FareVo;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 仓库信息
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-04 18:29:13
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageByCondition(Map<String, Object> params);

    FareVo getfreight(Long addrId);
}

