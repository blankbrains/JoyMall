package com.joy.joymall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/7/14 20:28
 */
@Configuration
public class JoyFeignConfig {


    @Bean
    public RequestInterceptor requestInterceptor() {
        //进行一次feign调用相当于更新一次请求域
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                //保存当前请求的数据
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (requestAttributes != null) {
                    HttpServletRequest request = requestAttributes.getRequest();
                    //同步请求头数据，保存用户登录信息能得到,将老请求的Cookie放入新请求
                    requestTemplate.header("Cookie", request.getHeader("Cookie"));
                }
            }
        };
    }
}
