package com.joy.joymall.seckill.feign;

import com.joy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/12 20:29
 */
@FeignClient("joymall-coupon")
public interface CouponFeignService {
    @PostMapping("/coupon/seckillsession/getLatestThreeDaysSecKillSku")
    R getLatestThreeDaysSecKillSku();
}
