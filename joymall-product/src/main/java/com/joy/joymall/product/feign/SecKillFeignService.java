package com.joy.joymall.product.feign;

import com.joy.common.utils.R;
import com.joy.joymall.product.feign.fallback.SecKillFeignServiceFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/14 21:21
 */
@FeignClient(value = "joymall-seckill",fallback = SecKillFeignServiceFallBack.class)
public interface SecKillFeignService {
    @GetMapping("/sku/seckill/{skuId}")
    R getSkuSecKillInfo(@PathVariable("skuId") Long skuId);
}
