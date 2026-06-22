package com.joy.joymall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.joy.common.to.es.SkuEsModel;
import com.joy.common.utils.R;
import com.joy.joymall.search.config.ElasticSearchConfig;
import com.joy.joymall.search.constant.EsConstant;
import com.joy.joymall.search.feign.ProductFeignService;
import com.joy.joymall.search.service.MallSearchService;
import com.joy.joymall.search.vo.SearchParam;
import com.joy.joymall.search.vo.SearchResult;
import com.joy.joymall.search.vo.attrvo.AttrResponseVo;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/29 23:37
 */
@Service
public class MallSearchServiceImpl implements MallSearchService {


    @Resource
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ProductFeignService productFeignService;

    /**
     * 得到检索的所有参数
     * 动态构建出DSL查询语句
     * <p>
     * 根据得到的所有参数，去es检索，得到结果后返回
     *
     * @param searchParam:查询条件
     * @return ： 返回检索的结果
     */
    @Override
    public SearchResult search(SearchParam searchParam) {
        SearchResult searchResult = new SearchResult();
        //1.构建查询条件
        SearchRequest searchRequest = buildSearchRequest(searchParam);
        try {
            //2.执行查询，得到结果
            SearchResponse searchResponse = restHighLevelClient.search(
                    searchRequest, ElasticSearchConfig.COMMON_OPTIONS);

            //3.分析响应数据
            searchResult = buildSearchResult(searchResponse, searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return searchResult;
    }

    /**
     * debug封装数据
     *
     * @param searchResponse：经过条件查询后的结果
     * @return ： 封装好的数据
     */
    private SearchResult buildSearchResult(SearchResponse searchResponse, SearchParam searchParam) {
        SearchResult result = new SearchResult();

        //1、返回的所有查询到的商品
        SearchHits hits = searchResponse.getHits();

        List<SkuEsModel> esModels = new ArrayList<>();
        //遍历所有商品信息
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);

                //判断是否按关键字检索，若是就显示高亮，否则不显示
                if (!StringUtils.isEmpty(searchParam.getKeyword())) {
                    //拿到高亮信息显示标题
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String skuTitleValue = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(skuTitleValue);
                }
                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);

        //2、当前商品涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        //获取属性信息的聚合
        ParsedNested attrsAgg = searchResponse.getAggregations().get("attrAgg");
        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attrIdAgg");
        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //1、得到属性的id
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);

            //2、得到属性的名字
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);

            //3、得到属性的所有值
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
            List<String> attrValues = attrValueAgg.getBuckets().stream().map(item -> item.getKeyAsString()).collect(Collectors.toList());
            attrVo.setAttrValues(attrValues);

            attrVos.add(attrVo);
        }

        result.setAttrVos(attrVos);

        //3、当前商品涉及到的所有品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        //获取到品牌的聚合
        ParsedLongTerms brandAgg = searchResponse.getAggregations().get("brandAgg");
        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();

            //1、得到品牌的id
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);

            //2、得到品牌的名字
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);

            //3、得到品牌的图片
            ParsedStringTerms brandImgAgg = bucket.getAggregations().get("brandImgAgg");
            String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }
        result.setBrandVos(brandVos);

        //4、当前商品涉及到的所有分类信息
        //获取到分类的聚合
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalogAgg = searchResponse.getAggregations().get("catalogAgg");
        for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //得到分类id
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));

            //得到分类名
            ParsedStringTerms catalogNameAgg = bucket.getAggregations().get("catalogNameAgg");
            String catalogName = catalogNameAgg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);
            catalogVos.add(catalogVo);
        }

        result.setCatalogVos(catalogVos);
        //===============以上可以从聚合信息中获取====================//
        //5、分页信息-页码
        result.setPageNum(searchParam.getPageNum());
        //5、1分页信息、总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);

        //5、2分页信息-总页码-计算
        int totalPages = (int) total % EsConstant.PAGE_SIZE == 0 ?
                (int) total / EsConstant.PAGE_SIZE : ((int) total / EsConstant.PAGE_SIZE + 1);
        result.setTotalPages(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);


        /**
         * 构建面包屑导航
         */
        List<SearchResult.NavVo> navVos = null;
        if (!CollectionUtils.isEmpty(searchParam.getAttrs())) {
            navVos = searchParam.getAttrs().stream().map((attr) -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] split = attr.split("_");
                //属性值
                navVo.setNavValue(split[1]);
                R r = productFeignService.getAttrInfo(Long.parseLong(split[0]));
                if (r.getCode() == 0) {
                    TypeReference<AttrResponseVo> typeReference = new TypeReference<AttrResponseVo>() {
                    };
                    AttrResponseVo attrResponseVo = r.getData(typeReference, "attr");
                    String attrName = attrResponseVo.getAttrName();
                    navVo.setNavName(attrName);
                } else {
                    navVo.setNavName(split[0]);
                }

                //当取消面包屑之后，将路径置空
                String encode = "";
                try {
                    encode = URLEncoder.encode(attr, "UTF-8");
                    encode.replace("+", "%20");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String replaceAttr = searchParam.getQueryString().replace("&attrs=" + encode, "");
                navVo.setLink("http://search.joymall.com/list.html?" + replaceAttr);
                return navVo;
            }).collect(Collectors.toList());

            result.setNavs(navVos);
        }


        return result;
    }


    /**
     * 传入请求参数，进行构建
     * SearchRequest对象，传入index，传入一个builder->builder可以构建查询，builder.query()，排序，builder.sort(),
     * 区间等等条件，每一个对应的又有一个builder，
     * 如查询的BoolQueryBuilder
     *
     * @param searchParam:请求参数
     * @return ：构建好的request
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {

        //构建查询语句
        SearchSourceBuilder builder = new SearchSourceBuilder();

        /**
         * 模糊匹配，过滤（按照属性，分类，品牌，价格区间，库存）
         */

        //多查询Builder
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //1.构建must--->搜索关键字
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", searchParam.getKeyword()));
        }
        //2.构建filter--->分类
        if (searchParam.getCatalog3Id() != null) {

            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId", searchParam.getCatalog3Id()));
        }

        //2.构建filter--->品牌
        if (searchParam.getBrandId() != null && searchParam.getBrandId().size() > 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", searchParam.getBrandId().toArray()));
        }


        //2.构建filter--->属性
        if (searchParam.getAttrs() != null && searchParam.getAttrs().size() > 0) {
            for (String attrString : searchParam.getAttrs()) { //类似1_CPU型号:骁龙  2_屏幕尺寸:5.5:6.0
                //批量查询
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();

                String[] attr = attrString.split("_");
                String arrrId = attr[0];
                String[] attrVal = attr[1].split(":");
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", arrrId));
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue", attrVal));
                //每一次循环都要嵌套一个query，而不是所有组合好之后形成一个query
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQueryBuilder.filter(nestedQuery);
            }
        }


        //2.构建filter--->库存

        if (searchParam.getHasStock() != null) {

            boolQueryBuilder.filter(QueryBuilders.termsQuery("hasStock", searchParam.getHasStock() == 1));
        }


        //2.构建filter--->价格区间
        if (!StringUtils.isEmpty(searchParam.getSkuPrice())) {
            RangeQueryBuilder rangePrice = QueryBuilders.rangeQuery("skuPrice");
            String[] price = searchParam.getSkuPrice().split("_");

            //两个区间
            if (price.length == 2) {  //_500长度为2
                if (searchParam.getSkuPrice().startsWith("_")) {
                    rangePrice.lte(price[1]);
                } else {
                    rangePrice.gte(price[0]).lte(price[1]);
                }
            } else if (price.length == 1) {  //单个区间
                rangePrice.gte(price[0]);
            }
            boolQueryBuilder.filter(rangePrice);
        }


        builder.query(boolQueryBuilder);

        /**
         * 排序，分页，高亮
         */

        //排序
        if (!StringUtils.isEmpty(searchParam.getSort())) {
            String sort = searchParam.getSort();
            String[] split = sort.split("_");
            String sortField = split[0];
            String sortRule = split[1];

            builder.sort(sortField, sortRule.equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC);
        }


        //分页:(页码-1) * 分页大小
        builder.from((searchParam.getPageNum() - 1) * EsConstant.PAGE_SIZE);
        builder.size(EsConstant.PAGE_SIZE);


        //高亮
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            builder.highlighter(highlightBuilder);
        }


        /**
         * 聚合分析
         */
        //品牌聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brandAgg");
        brandAgg.field("brandId");
        brandAgg.size(EsConstant.AGG_SIZE);

        //品牌下子聚合
        brandAgg.subAggregation(AggregationBuilders
                .terms("brandNameAgg").field("brandName").size(EsConstant.AGG_SIZE));
        brandAgg.subAggregation(AggregationBuilders
                .terms("brandImgAgg").field("brandImg").size(EsConstant.AGG_SIZE));
        builder.aggregation(brandAgg);


        //分类聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalogAgg");
        catalogAgg.field("catalogId");
        catalogAgg.size(EsConstant.AGG_SIZE);
        //分类下子聚合
        catalogAgg.subAggregation(AggregationBuilders
                .terms("catalogNameAgg").field("catalogName").size(EsConstant.AGG_SIZE));

        builder.aggregation(catalogAgg);

        //属性聚合,属性是个嵌套聚合，attrAgg下面子聚合是attrIdAgg，其下又有两个
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attrAgg", "attrs");
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attrIdAgg");
        attrIdAgg.field("attrs.attrId");
        attrIdAgg.size(EsConstant.AGG_SIZE);


        //属性子聚合
        attrIdAgg.subAggregation(AggregationBuilders
                .terms("attrNameAgg").field("attrs.attrName").size(EsConstant.AGG_SIZE));

        attrIdAgg.subAggregation(AggregationBuilders
                .terms("attrValueAgg").field("attrs.attrValue").size(EsConstant.AGG_SIZE));

        //大嵌套聚合下的子聚合
        attrAgg.subAggregation(attrIdAgg);

        builder.aggregation(attrAgg);

        SearchRequest request = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, builder);


        return request;
    }
}


