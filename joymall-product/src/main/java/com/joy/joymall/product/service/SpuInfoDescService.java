package com.joy.joymall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.joy.common.utils.PageUtils;
import com.joy.joymall.product.entity.SpuInfoDescEntity;

import java.util.Map;

/**
 * spu信息介绍
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-03 22:33:57
 */
public interface SpuInfoDescService extends IService<SpuInfoDescEntity> {

    void saveSpuInfoDesc(SpuInfoDescEntity spuInfoDescEntity);

    PageUtils queryPage(Map<String, Object> params);
}

