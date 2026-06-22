package com.joy.joymall.coupon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.joy.joymall.coupon.dao")
public class JoymallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(JoymallCouponApplication.class, args);
    }

}
