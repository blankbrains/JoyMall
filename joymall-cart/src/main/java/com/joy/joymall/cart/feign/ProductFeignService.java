package com.joy.joymall.cart.feign;

import com.joy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/9 20:15
 */
@FeignClient("joymall-product")
public interface ProductFeignService {
    @PostMapping("/product/skuinfo/info/{skuId}")
    R getSkuinfo(@PathVariable("skuId") Long skuId);

    @GetMapping("/product/skusaleattrvalue/stringlist/{skuId}")
    List<String> getSkuSaleAttrValues(@PathVariable("skuId") Long skuId);


    @GetMapping("/product/skuinfo/{skuId}/getCurrentPrice")
    BigDecimal getCurrentPrice(@PathVariable("skuId") Long skuId);
}
