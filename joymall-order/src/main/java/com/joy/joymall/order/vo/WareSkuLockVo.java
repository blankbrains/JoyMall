package com.joy.joymall.order.vo;

import io.swagger.annotations.ApiModel;

import lombok.Data;

import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/7/25 21:24
 */
@Data
@ApiModel("锁定库存需要的Vo")
public class WareSkuLockVo {

    private String orderSn;

    private List<OrderItemVo> lockVos;
}
