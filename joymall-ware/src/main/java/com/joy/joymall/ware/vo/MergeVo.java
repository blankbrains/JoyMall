package com.joy.joymall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/21 15:53
 */
@Data
public class MergeVo {

    private Long purchaseId;

    private List<Long> items;
}
