package com.joy.joymall.ware.vo;

import lombok.Data;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/24 22:53
 */
@Data
public class SkuHasStockVo {
    private Long skuId;

    private boolean hasStock;
}
