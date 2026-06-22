package com.joy.joymall.product.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/12 22:50
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AttrResponseVo extends AttrVo {
    /**
     * 分类名称
     */
    private String catelogName;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 回显分类路径
     */
    private Long[] catelogPath;


}
