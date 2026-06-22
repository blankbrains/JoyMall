package com.joy.joymall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;


@EnableRedisHttpSession
@SpringBootApplication
@MapperScan("com.joy.joymall.product.dao")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.joy.joymall.product.feign")
@EnableCaching
public class JoymallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(JoymallProductApplication.class, args);
    }

}
