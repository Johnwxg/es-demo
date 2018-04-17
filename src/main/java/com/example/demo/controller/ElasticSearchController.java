/**
 * Copyright (C), 2018-2018
 * FileName: ElasticSearchController
 * Author:   WXG
 * Date:     2018/4/16 20:55
 * Description: ES控制器
 */
package com.example.demo.controller;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 〈ES控制器〉
 *
 * @Author wxg
 * @Date 2018/4/16
 * @since 1.0.0
 */
@RestController
@RequestMapping(value = "/esCtl")
public class ElasticSearchController {

    @Autowired
    TransportClient client;

    /**
     * 增加操作
     * @param title
     * @param author
     * @param wordCount
     * @param publishDate
     * @return
     */
    @PostMapping("/add")
    public ResponseEntity add( @RequestParam(name = "title") String title, @RequestParam(name = "authro") String author,
            @RequestParam(name = "word_count") int wordCount,
            @RequestParam(name = "publish_date") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")Date publishDate ){
        try {
            XContentBuilder content = XContentFactory.jsonBuilder().startObject()
                    .field("title", title)
                    .field("author", author)
                    .field("word_count", wordCount)
                    .field("publish_date", publishDate)
                    .endObject();

            IndexResponse result = this.client.prepareIndex("book", "novel").setSource(content).get();
            return new ResponseEntity(result.getId(), HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 删除操作
     * @param id
     * @return
     */
    @DeleteMapping("/delete")
    public ResponseEntity delete(@RequestParam(name = "id") String id){
        DeleteResponse result = client.prepareDelete("book", "novel", id).get();
        return new ResponseEntity(result.getResult().toString(), HttpStatus.OK);
    }

    /**
     * 查询操作
     * @param id
     * @return
     */
    @GetMapping("/get")
    public ResponseEntity get(@RequestParam(name = "id", defaultValue="") String id){
        if (id.isEmpty())
        {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        GetResponse result = this.client.prepareGet("book", "novel", id).get();
        if (!result.isExists())
        {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(result.getSource(), HttpStatus.OK);
    }


    /**
     * 更新操作
     * @param id
     * @param title
     * @param author
     * @return
     */
    @PutMapping("/put")
    public ResponseEntity update(@RequestParam(name = "id") String id,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "author", required = false) String author){

        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
            if (title!= null)
            {
                builder.field("title", title);
            }
            if (author != null)
            {
                builder.field("author", author);
            }
            builder.endObject();
            UpdateRequest updateRequest = new UpdateRequest("book", "novel", id);
            updateRequest.doc(builder);

            UpdateResponse result = client.update(updateRequest).get();

            return new ResponseEntity(result.getResult().toString(), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/query")
    public ResponseEntity query(@RequestParam(name = "author", required = false) String author,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "gt_word_count", defaultValue = "0") int gtWordCount,
            @RequestParam(name = "lt_word_count", required = false) Integer ltWordCount){

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (author != null)
        {
            boolQueryBuilder.must(QueryBuilders.matchQuery("author",author));
        }
        if (title != null)
        {
            boolQueryBuilder.must(QueryBuilders.matchQuery("title", title));
        }

        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("word_count").from(gtWordCount);
        if (ltWordCount != null && ltWordCount > 0)
        {
            rangeQueryBuilder.to(ltWordCount);
        }

        boolQueryBuilder.filter(rangeQueryBuilder);

        SearchRequestBuilder searchRequestBuilder = this.client.prepareSearch("book")
                .setTypes("novel")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(boolQueryBuilder)
                .setFrom(0)
                .setSize(10);
        System.out.println(searchRequestBuilder); //调试用

        SearchResponse response = searchRequestBuilder.get();
        List<Map<String, Object>> result = new ArrayList<>();
        for (SearchHit hit : response.getHits())
        {
            result.add(hit.getSource());
        }

        return  new ResponseEntity(result, HttpStatus.OK);
    }
    /*
    上面的代码组织的复合查询类似下面的Query DSL：
     {
        "query":{
            "bool":{
                "must":[
                    {"match":{"author":"张三"}},
                    {"match":{"title":"Elasticsearch"}}
                ],
                "filter":[
                    {"range":
                        {"word_count":{
                                "gt":"0",
                                "lt":"3000"
                            }
                        }
                    }
                ]
            }
        }
    }
    */
}
