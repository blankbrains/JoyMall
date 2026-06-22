package com.joy.common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Description: 不同服务之间传输数据用To传输
 * @Author:joymall
 * @Date:2022/5/20 23:08
 */

@Data
public class SpuBoundsTo {
    private Long spuId;

    private BigDecimal buyBounds;

    private BigDecimal growBounds;
}
