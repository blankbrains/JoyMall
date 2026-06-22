package com.joy.common.to.mq;

import lombok.Data;

import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/2 21:29
 */
@Data
public class StockLockedTo {

    /**
     * 库存工作单
     */
    private Long id;
    /**
     * 库存工作单详情
     */
    private StockDetailTo detailTo;

}
