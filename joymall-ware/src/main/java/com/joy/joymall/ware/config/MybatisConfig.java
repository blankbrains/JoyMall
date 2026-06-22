package com.joy.joymall.ware.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/11 21:00
 */
@Configuration
//开启事务
@EnableTransactionManagement
@MapperScan("com.joy.joymall.ware.dao")
public class MybatisConfig {

    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();

        //达到请求最大页后，true返回首页，false继续请求
        paginationInterceptor.setOverflow(true);
        paginationInterceptor.setLimit(1000L);
        return paginationInterceptor;
    }
}
