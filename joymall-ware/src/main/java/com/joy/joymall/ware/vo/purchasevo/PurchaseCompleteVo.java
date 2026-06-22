package com.joy.joymall.ware.vo.purchasevo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/21 20:31
 */
@Data
public class PurchaseCompleteVo {

    /**
     * 采购单id
     */
    @NotNull
    private Long id;

    private List<PurchaseItemVo> items;

}
