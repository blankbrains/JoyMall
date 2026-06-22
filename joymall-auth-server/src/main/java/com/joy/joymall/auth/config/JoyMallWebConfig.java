package com.joy.joymall.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Description:
 * 避免controller的空方法
 * @Author:joymall
 * @Date:2022/6/5 0:34
 */
@Configuration
public class JoyMallWebConfig implements WebMvcConfigurer {
    /**
     * urlPath=请求路径
     * viewName=返回地址
     * @param registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login.html").setViewName("login");
        registry.addViewController("register.html").setViewName("register");
    }
}
