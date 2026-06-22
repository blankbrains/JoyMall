package com.joy.joymall.seckill.feign;

import com.joy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @Description: 远程调用只要路径对，参数对就可以
 * @Author:joymall
 * @Date:2022/9/12 22:13
 */
@FeignClient("joymall-product")
public interface ProductFeignService {
    @PostMapping("/product/skuinfo/info/{skuId}")
    R getSkuInfo(@PathVariable("skuId") Long skuId);
}
