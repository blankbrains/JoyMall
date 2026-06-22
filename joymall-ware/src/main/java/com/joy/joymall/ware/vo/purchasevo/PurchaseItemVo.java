package com.joy.joymall.ware.vo.purchasevo;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/21 20:33
 */
@Data
public class PurchaseItemVo {

    /**
     * 每个采购项id
     */
    @NotNull
    private Long itemId;

    /**
     * 采购项状态
     */
    private Integer status;

    /**
     * 采购失败原因
     */
    private String reason;
}
