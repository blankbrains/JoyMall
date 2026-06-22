package com.joy.joymall.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

import com.joy.common.valid.AddGroup;
import com.joy.common.valid.ListValue;
import com.joy.common.valid.UpdateGroup;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;

/**
 * 品牌
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-03 22:33:57
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 品牌id
     */
    @Null(message = "新增不能指定品牌id", groups = AddGroup.class)
    @NotNull(message = "修改必须指定品牌id", groups = UpdateGroup.class)
    @TableId
    private Long brandId;
    /**
     * 品牌名
     */
    @NotBlank(message = "品牌名不能为空", groups = {AddGroup.class, UpdateGroup.class})
    private String name;
    /**
     * 品牌logo地址
     */
    @NotEmpty(message = "品牌logo不能为空且为合法url地址", groups = {AddGroup.class})
    @URL(message = "品牌logo必须为合法的url地址", groups = {AddGroup.class})
    private String logo;
    /**
     * 介绍
     */
    private String descript;
    /**
     * 显示状态[0-不显示；1-显示]
     */
    @NotNull(message = "状态不能为空")
    @ListValue(values={0,1},groups = {AddGroup.class,UpdateGroup.class})
    private Integer showStatus;
    /**
     * 检索首字母
     */
    @NotEmpty(message = "检索字母不为空且为一个字母", groups = {AddGroup.class})
    @Pattern(regexp = "^[a-zA-z]$", message = "检索字母必须为一个字母", groups = {AddGroup.class, UpdateGroup.class})
    private String firstLetter;
    /**
     * 排序
     */
    @NotNull(message = "排序必须为数字且不为空", groups = {AddGroup.class})
    @Min(value = 0L, message = "排序必须大于等于0", groups = {AddGroup.class, UpdateGroup.class})
    private Integer sort;

}
