package com.joy.joymall.member.feign;

import com.joy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


import java.util.Map;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/7 22:21
 */
@FeignClient("joymall-order")
public interface OrderFeignService {
    @PostMapping("/order/order/listWithItem")
    R listWithItem(@RequestBody Map<String, Object> params);
}
