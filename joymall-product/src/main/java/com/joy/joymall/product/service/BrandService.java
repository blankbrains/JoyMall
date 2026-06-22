package com.joy.joymall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.joy.common.utils.PageUtils;
import com.joy.joymall.product.entity.BrandEntity;

import java.util.Map;

/**
 * 品牌
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-03 22:33:57
 */
public interface BrandService extends IService<BrandEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void updateDetails(BrandEntity brand);
}

