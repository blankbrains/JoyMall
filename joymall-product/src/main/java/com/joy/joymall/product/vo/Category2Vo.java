package com.joy.joymall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/25 21:17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Category2Vo {
    /**
     * 一级父分类
     */
    private String catagory1Id;

    /**
     * 三级子分类
     */
    private List<Category2Vo.Category3Vo> category3List;

    private String id;

    private String name;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Category3Vo{
        /**
         * 二级父分类
         */
        private String catagory2Id;
        private String id;
        private String name;
    }

}
