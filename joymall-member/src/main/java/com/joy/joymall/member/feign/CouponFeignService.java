package com.joy.joymall.member.feign;

import com.joy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/4 20:47
 */
@FeignClient("joymall-coupon")
public interface CouponFeignService {

    @RequestMapping("coupon/coupon/member/list")
    public R memberCoupon();
}
