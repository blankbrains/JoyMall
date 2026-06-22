package com.joy.joymall.product.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.joy.joymall.product.entity.AttrEntity;
import lombok.Data;

import java.util.List;

/**
 * @Description: 通过分类id得到分组再得到所有属性的vo展示
 * @Author:joymall
 * @Date:2022/5/19 20:29
 */
@Data
public class AttrGroupWithAttrsVo {
    private static final long serialVersionUID = 1L;

    /**
     * 分组id
     */
    private Long attrGroupId;
    /**
     * 组名
     */
    private String attrGroupName;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 描述
     */
    private String descript;
    /**
     * 组图标
     */
    private String icon;
    /**
     * 所属分类id
     */
    private Long catelogId;

    /**
     * 当前属性分组的所有属性
     */
    private List<AttrEntity> attrs;

}
