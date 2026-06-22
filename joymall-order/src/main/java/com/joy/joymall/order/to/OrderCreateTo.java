package com.joy.joymall.order.to;

import com.joy.joymall.order.entity.OrderEntity;
import com.joy.joymall.order.entity.OrderItemEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/7/24 19:03
 */
@ApiModel("订单总数据")
@Data
public class OrderCreateTo {

    @ApiModelProperty("订单数据")
    private OrderEntity order;

    @ApiModelProperty("订单项数据")
    private List<OrderItemEntity> orderItems;

    @ApiModelProperty("订单应付价格")
    private BigDecimal payPrice;

    @ApiModelProperty("运费")
    private BigDecimal fare;

}
