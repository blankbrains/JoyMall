package com.joy.joymall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/7/20 22:06
 */
@Data
public class FareVo {

    private MemberReceiveAddressVo memberReceiveAddressVo;

    private BigDecimal fare;
}
