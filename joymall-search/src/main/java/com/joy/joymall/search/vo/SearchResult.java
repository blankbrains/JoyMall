package com.joy.joymall.search.vo;

import com.joy.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/30 0:03
 */
@Data
public class SearchResult {

    /**
     * 商品基本信息
     */
    private List<SkuEsModel> products;


    /**
     * 当前页码
     */
    private Integer pageNum;


    /**
     * 总记录数
     */
    private Long total;


    /**
     * 总页码
     */
    private Integer totalPages;


    /**
     * 导航页
     */
    private List<Integer> pageNavs;


    /**
     * 面包屑导航
     */
    List<NavVo> navs;


    /**
     * 当前查询结果所有涉及到的品牌
     * 小米，华为等等
     */
    private List<BrandVo> brandVos;


    /**
     * 当前查询结果所有涉及的属性
     * 比如手机有操作系统，分辨率等等
     */
    private List<AttrVo> attrVos;


    /**
     * 查询的所有分类
     */
    private List<CatalogVo> catalogVos;


    @Data
    public static class BrandVo {
        private Long BrandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static class AttrVo {
        private Long attrId;
        private String attrName;
        private List<String> attrValues;
    }


    @Data
    public static class CatalogVo {
        private Long catalogId;
        private String catalogName;
    }

    @Data
    public static class NavVo {
        private String navName;
        private String navValue;
        private String link;
    }
}
