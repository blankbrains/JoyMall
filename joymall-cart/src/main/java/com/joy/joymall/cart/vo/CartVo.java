package com.joy.joymall.cart.vo;

import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description: 每个用户对应的购物车
 * @Author:joymall
 * @Date:2022/6/8 22:40
 */
@Data
public class CartVo {

    /**
     * 当前购物车的项数
     */
    private Integer countNum;


    /**
     * 商品类型数量
     */
    private Integer countType;


    /**
     * 每个项的数据
     */
    List<CartItemVo> items;


    /**
     * 所有商品总价
     */
    private BigDecimal totalAmount;


    /**
     * 优惠的价格
     */
    private BigDecimal reduceAmount = new BigDecimal(0);


    public Integer getCountNum() {
        this.countNum = 0;
        if (!CollectionUtils.isEmpty(items)) {
            for (CartItemVo item : items) {
                this.countNum += item.getCount();
            }
        }
        return countNum;
    }


    public Integer getCountType() {
        if (CollectionUtils.isEmpty(items)) {
            return 0;
        }
        return items.size();
    }


    public BigDecimal getTotalAmount() {
        this.totalAmount = new BigDecimal(0);
        //计算购物项总价
        if (!CollectionUtils.isEmpty(items)) {
            for (CartItemVo item : items) {
                if(item.getCheck()){
                    totalAmount = totalAmount.add(item.getTotalPrice());
                }
            }
        }
        //减去优惠
        totalAmount = totalAmount.subtract(reduceAmount);
        return totalAmount;
    }
}
