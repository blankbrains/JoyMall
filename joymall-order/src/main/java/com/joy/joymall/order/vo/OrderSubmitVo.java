package com.joy.joymall.order.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/7/24 18:15
 */
@Data
@ApiModel("订单提交页面的数据")
public class OrderSubmitVo {

    /**
     * 收获地址id
     */
    private Long addrId;


    /**
     * 支付方式
     */
    private Integer payType = 1;


    // 无需提交购买的商品，去购物车再获取一遍即可
    /**
     * 防重令牌
     */
    private String orderToken;


    /**
     * 应付价格
     */
    private BigDecimal payPrice;
}

