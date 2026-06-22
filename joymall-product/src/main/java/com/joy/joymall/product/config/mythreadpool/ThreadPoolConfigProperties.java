package com.joy.joymall.product.config.mythreadpool;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Description:
 * 可动态配置线程池参数的配置类
 * @Author:joymall
 * @Date:2022/6/4 17:50
 */
@ConfigurationProperties(prefix = "joymall.threadpool")
@Component
@Data
public class ThreadPoolConfigProperties {
    private Integer coreSize;
    private Integer maxSize;
    private Integer keepAliveTime;
}
