package com.joy.joymall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/7/24 19:18
 */
@Data
public class FareVo {
    private MemberAddressVo memberAddressVo;

    private BigDecimal fare;
}
