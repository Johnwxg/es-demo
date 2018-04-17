/**
 * Copyright (C), 2018-2018
 * FileName: ElasticSearchConfig
 * Author:   WXG
 * Date:     2018/4/16 20:33
 * Description: ES配置文件
 */
package com.example.demo.config;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 〈ES配置文件〉
 *
 * @Author wxg
 * @Date 2018/4/16
 * @since 1.0.0
 */
@Configuration
public class ElasticSearchConfig {

    /**
     *
     * @return
     */
    @Bean(name = "client")
    public TransportClient getClient(){
        InetSocketTransportAddress node = null;
        try {
            node = new InetSocketTransportAddress(InetAddress.getByName("192.168.168.128"), 9300);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        //初始化默认的client
        Settings settings = Settings.builder().put("cluster.name","my-es").build();
        TransportClient client = new PreBuiltTransportClient(settings);
        client.addTransportAddress(node);
        return client;
    }
}
