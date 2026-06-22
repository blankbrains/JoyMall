package com.joy.joymall.search.feign;

import com.joy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/1 23:21
 */
@FeignClient("joymall-product")
public interface ProductFeignService {
    @PostMapping("/product/attr/info/{attrId}")
    public R getAttrInfo(@PathVariable("attrId") Long attrId);
}
