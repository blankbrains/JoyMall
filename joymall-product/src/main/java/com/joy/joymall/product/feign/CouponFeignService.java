package com.joy.joymall.product.feign;

import com.joy.common.to.SkuReductionTo;
import com.joy.common.to.SpuBoundsTo;
import com.joy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/20 23:01
 */
@FeignClient("joymall-coupon")
public interface CouponFeignService {



    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundsTo spuBoundsTo);

    @PostMapping("/coupon/skufullreduction/saveskuinfo")
    R saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);
}
