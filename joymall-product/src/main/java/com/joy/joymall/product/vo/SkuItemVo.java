package com.joy.joymall.product.vo;

import com.joy.joymall.product.entity.SkuImagesEntity;
import com.joy.joymall.product.entity.SkuInfoEntity;
import com.joy.joymall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/2 23:53
 */
@Data
public class SkuItemVo {

    /**
     * Sku基本信息
     */
    private SkuInfoEntity info;


    /**
     * 是否有货
     */
    boolean hasStock = true;

    /**
     * sku图片信息
     */
    private List<SkuImagesEntity> images;


    /**
     * spu销售组合属性
     */
    private List<SkuItemSaleAttrVo> saleAttr;

    /**
     * spu介绍
     */
    private SpuInfoDescEntity desc;


    /**
     * spu规格参数信息
     */
    private List<SpuItemAttrGroupVo> groupAttrs;

    /**
     * sku秒杀信息
     */
    private SecKillInfoVo secKillInfoVo;

    @Data
    public static class SkuItemSaleAttrVo {
        /**
         * 属性id
         */
        private Long attrId;


        /**
         * 属性名
         */
        private String attrName;


        /**
         * 每个属性值下面对应的SkuId,如颜色有多少种Sku
         */
        private List<AttrValueWithSkuIdVo> attrValues;

    }


    @Data
    private static class AttrValueWithSkuIdVo {
        /**
         * 属性值，如颜色
         */
        private String attrValue;

        /**
         * 对应的SkuId
         */
        private String skuIds;
    }


    @ToString
    @Data
    public static class SpuItemAttrGroupVo {
        /**
         * spu组名
         */
        private String groupName;


        /**
         * spu当前组对应的所有属性值
         */
        private List<SpuBaseAttrVo> baseAttr;

    }


    @ToString
    @Data
    private static class SpuBaseAttrVo {
        /**
         * 基本属性名
         */
        private String attrName;


        /**
         * 基本属性值
         */
        private String attrValue;
    }
}
