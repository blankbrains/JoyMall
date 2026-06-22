package com.joy.common.exception;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/8/23 18:01
 */
public class NoStockException extends RuntimeException {
    private Long skuId;

    public NoStockException(Long skuId){
        super(skuId + ":库存不足！");
        this.skuId = skuId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
}
