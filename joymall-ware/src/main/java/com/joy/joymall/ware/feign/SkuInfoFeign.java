package com.joy.joymall.ware.feign;

import com.joy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/21 21:05
 */
@FeignClient("joymall-product")
public interface SkuInfoFeign {
    /**
     * 可以给网关发加api前缀，也可以之间发给服务
     * @param skuId
     * @return
     */
    @PostMapping("/product/skuinfo/info/{skuId}")
    R info(@PathVariable("skuId") Long skuId);
}
