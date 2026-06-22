package com.joy.joymall.order.vo;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/27 18:41
 */
@Data
public class OrderConfirmVo {

    /**
     * 收货地址
     */
    private List<MemberAddressVo> address;

    /**
     * 所有选中的购物项
     */
    private List<OrderItemVo> items;


    /**
     * 是否有库存
     */
    private Map<Long,Boolean> hasStock;


    /**
     * 发票信息...
     */

    /**
     * 优惠券
     */
    private Integer integration;


    /**
     * 订单总额
     */
    private BigDecimal total;

    //无法用total，自定义get方法之后就无法用原来的变量了
    public BigDecimal getTotal() {
        BigDecimal total = new BigDecimal(0);
        if (!CollectionUtils.isEmpty(items)) {
            for (OrderItemVo item : items) {
                total = total.add(item.getTotalPrice());
            }
        }
        return total;
    }


    /**
     * 商品件数
     */
    public Integer getCount() {
        Integer count = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    /**
     * 应付价格
     */
    private BigDecimal payPrice;

    public BigDecimal getPayPrice() {
        BigDecimal payPrice = new BigDecimal(0);
        if (!CollectionUtils.isEmpty(items)) {
            for (OrderItemVo item : items) {
                payPrice = payPrice.add(item.getTotalPrice());
            }
        }
        return payPrice;
    }


    /**
     * 带一个令牌，避免重复提交购物车（因网络等原因）
     */
    private String orderToken;
}
