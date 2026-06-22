package com.joy.joymall.order.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/7/25 21:29
 */
@Data
@ApiModel("锁定库存结果")
public class WareSkuLockRespVo {

    private Long skuId;


    @ApiModelProperty("锁定多少件")
    private Integer count;


    @ApiModelProperty("是否锁定成功")
    private boolean locked;
}
