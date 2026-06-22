package com.joy.joymall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/29 23:35
 */
@Data
public class SearchParam {

    /**
     * 检索关键参数
     */
    private String keyword;


    /**
     * 三级分类id
     */
    private Long catalog3Id;


    /**
     * 排序条件
     */
    private String sort;


    //过滤条件
    /**
     * 是否有货,默认有货
     */
    private Integer hasStock;


    /**
     * 价格区间
     */
    private String skuPrice;


    /**
     * 品牌名称
     */
    private List<Long> brandId;


    /**
     * 属性条件：如安装，5G等
     */
    private List<String> attrs;


    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 查询条件
     */

    private String queryString;
}
