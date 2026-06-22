package com.joy.joymall.order.feign;

import com.joy.common.utils.R;
import com.joy.joymall.order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/7/20 21:14
 */
@FeignClient("joymall-ware")
public interface WareFeignService {
    @PostMapping("/ware/waresku/hasstock")
    R getSkuHasStock(@RequestBody List<Long> skuIds);


    @GetMapping("/ware/wareinfo/freight")
    R getFreight(@RequestParam("addrId") Long addrId);


    @PostMapping("/ware/waresku/lock")
    R orderLockStock(@RequestBody WareSkuLockVo wareSkuLockVo);
}
