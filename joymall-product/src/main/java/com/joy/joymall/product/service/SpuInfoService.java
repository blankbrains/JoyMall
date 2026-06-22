package com.joy.joymall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.joy.common.utils.PageUtils;
import com.joy.joymall.product.entity.SpuInfoDescEntity;
import com.joy.joymall.product.entity.SpuInfoEntity;
import com.joy.joymall.product.vo.SpuSaveVo;

import java.util.Map;

/**
 * spu信息
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-03 22:33:57
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo spuSaveVo);

    void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity);

    PageUtils queryPageByCondition(Map<String, Object> params);

    void up(Long spuId);

    SpuInfoEntity getSpuBySkuId(Long skuId);
}

