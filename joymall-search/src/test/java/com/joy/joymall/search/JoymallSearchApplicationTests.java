package com.joy.joymall.search;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;

@SpringBootTest
class JoymallSearchApplicationTests {

    @Resource
    RestHighLevelClient restHighLevelClient;

    @Test
    void contextLoads() {
        System.out.println(restHighLevelClient);
    }

    @Test
    void loadData() throws IOException {
        User user = new User("测试用户", 21, "男");
        IndexRequest indexRequest = new IndexRequest("users")
                .id("1")
                .source(JSON.toJSONString(user), XContentType.JSON);
        IndexResponse response = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    @Data
    @AllArgsConstructor
    static class User {
        private String userName;
        private int age;
        private String gender;
    }

    @Test
    void searchData() throws IOException {
        SearchRequest searchRequest = new SearchRequest();

        //创建检索和条件
        searchRequest.indices("bank");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder);
        //查询条件
        searchSourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));
        //聚合

        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("aggAgg").field("age").size(10);
        searchSourceBuilder.aggregation(ageAgg);

        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        searchSourceBuilder.aggregation(balanceAvg);

        System.out.println("检索条件:" + searchSourceBuilder);
        //执行检索
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

//        SearchHits hits = search.getHits();
//        SearchHit[] hitsHits = hits.getHits();
//
//        for (SearchHit hitsHit : hitsHits) {
//            System.out.println(hitsHit.getSourceAsString());
//        }
        System.out.println("检索结果:" + response.toString());


        Aggregations aggregations = response.getAggregations();
        Terms ageAvg = aggregations.get("ageAvg");
        ageAvg.getBuckets().forEach((bucket) -> {

        });
    }


    @Test
    void testLength(){
        String s = "500_";
        String[] length = s.split("_");
        System.out.println(length.length);
    }

}
