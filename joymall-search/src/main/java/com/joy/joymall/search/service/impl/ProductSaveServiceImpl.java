package com.joy.joymall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.joy.common.to.es.SkuEsModel;
import com.joy.joymall.search.config.ElasticSearchConfig;
import com.joy.joymall.search.constant.EsConstant;
import com.joy.joymall.search.service.ProductSaveService;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;

import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/25 0:02
 */
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

    @Resource
    private RestHighLevelClient restHighLevelClient;

    @Override
    public void productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        //1.建立索引和映射关系
        //2.批量保存数据
        BulkRequest bulkRequest = new BulkRequest();
        skuEsModels.forEach((skuEsModel -> {
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(skuEsModel.getSkuId().toString());
            String toJSONString = JSON.toJSONString(skuEsModel);
            indexRequest.source(toJSONString, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }));
        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, ElasticSearchConfig.COMMON_OPTIONS);

        boolean hasFailures = bulkResponse.hasFailures();
        //TODO 批量是否有错误，有再处理
    }
}
