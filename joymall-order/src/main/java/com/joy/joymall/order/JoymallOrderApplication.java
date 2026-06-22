package com.joy.joymall.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;


@EnableRabbit
@SpringBootApplication
@MapperScan("com.joy.joymall.order.dao")
@EnableDiscoveryClient
@EnableRedisHttpSession
@EnableFeignClients
@EnableAspectJAutoProxy(exposeProxy = true)
public class JoymallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(JoymallOrderApplication.class, args);
    }

}
