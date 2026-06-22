package com.joy.joymall.order.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/27 18:47
 */
@Data
@ToString
public class OrderItemVo {
    private Long skuId;


    private String title;


    private String image;


    /**
     * 某件商品多个属性，比如电脑的CPU型号，内存大小
     */
    private List<String> skuAttr;


    private BigDecimal price;


    private Integer count;


    private BigDecimal totalPrice;


    // TODO 待查询库存


    private BigDecimal skuWeight;

    public BigDecimal getTotalPrice() {
        if (this.price == null) {
            this.price = new BigDecimal(0);
        }
        return this.price.multiply(new BigDecimal(count));
    }

}
