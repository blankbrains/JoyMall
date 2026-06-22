package com.joy.common.to;

import lombok.Data;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/24 22:53
 */
@Data
public class SkuHasStockTo {
    private Long skuId;

    private boolean hasStock;
}
