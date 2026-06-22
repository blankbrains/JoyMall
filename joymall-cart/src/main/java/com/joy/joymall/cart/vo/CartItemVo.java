package com.joy.joymall.cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description: 购物车中对应的购物项
 * @Author:joymall
 * @Date:2022/6/8 22:40
 */
@Data
public class CartItemVo {


    private Long skuId;

    /**
     * 当前购物车是否被选中结账
     */
    private Boolean check = true;


    private String title;


    private String image;


    /**
     * 某件商品多个属性，比如电脑的CPU型号，内存大小
     */
    private List<String> skuAttr;


    private BigDecimal price;


    private Integer count;


    private BigDecimal totalPrice;

    public BigDecimal getTotalPrice() {
        return this.price.multiply(new BigDecimal(count));
    }

    public void setTotalPrice() {
        this.totalPrice = price.multiply(new BigDecimal(count));
    }
}
