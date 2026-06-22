package com.joy.joymall.member;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@EnableRedisHttpSession
@SpringBootApplication
@MapperScan("com.joy.joymall.member.dao")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.joy.joymall.member.feign")
public class JoymallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(JoymallMemberApplication.class, args);
    }

}
