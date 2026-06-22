package com.joy.joymall.order.vo;

import com.joy.joymall.order.entity.OrderEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/7/24 18:37
 */
@ApiModel("订单提交相应数据")
@Data
public class OrderSubmitRespVo {

    @ApiModelProperty("订单信息")
    private OrderEntity orderEntity;

    @ApiModelProperty("错误状态码，只要不是0就错误")
    private Integer code;


}
