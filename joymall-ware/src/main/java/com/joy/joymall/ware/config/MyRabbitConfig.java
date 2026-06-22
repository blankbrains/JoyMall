package com.joy.joymall.ware.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/1 23:33
 */
@Configuration
public class MyRabbitConfig {

    /**
     * 交换机
     */
    public static final String EXCHANGE = "stock-event-exchange";
    /**
     * 死信队列
     */
    public static final String DEAD_QUEUE = "stock.release.stock.queue";
    /**
     * 延时队列
     */
    public static final String DELAY_QUEUE = "stock.delay.queue";
    /**
     * 延时队列的消息通过交换机发送给死信队列的routing-key
     */
    private static final String EXCHANGE_ROUTING_DEAD_QUEUE_ROUTING_KEY = "stock.release";
    /**
     * 延时队列消息存活时间
     */
    private static final int DELAY_QUEUE_TTL = 1000 * 60 * 2;
    /**
     * 交换机绑定延时队列的Routing-key
     */
    public static final String EXCHANGE_BINDING_DELAY_QUEUE_ROUTING_KEY = "stock.locked";
    /**
     * 交换机绑定死信队列的Routing-key
     */
    public static final String EXCHANGE_BINDING_DEAD_QUEUE_ROUTING_KEY = "stock.release.#";

    /**
     * 自制消息传输格式
     *
     * @return ： json格式
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    /**
     * 交换机
     *
     * @return ：
     */
    @Bean("exchange")
    public Exchange stockEventExchange() {

        return new TopicExchange(EXCHANGE, true, false);
    }

    /**
     * @return ：死信队列
     */
    @Bean("stockReleaseStockQueue")
    public Queue stockReleaseStockQueue() {
        return new Queue(DEAD_QUEUE, true, false, false);
    }


    /**
     * @return ： 延时队列
     */
    @Bean("stockDelayQueue")
    public Queue stockDelayQueue() {
        Map<String, Object> args = new HashMap<>(3);
        //消息到时间路由到那个交换机
        args.put("x-dead-letter-exchange", EXCHANGE);
        //路由到交换机通过那个routing-key发送给队列
        args.put("x-dead-letter-routing-key", EXCHANGE_ROUTING_DEAD_QUEUE_ROUTING_KEY);
        //消息过期时间
        args.put("x-message-ttl", DELAY_QUEUE_TTL);
        return new Queue(DELAY_QUEUE, true, false, false, args);
    }


    @Bean
    public Binding stockLockedBinding(@Qualifier("stockDelayQueue") Queue delayQueue,
                                    @Qualifier("exchange") Exchange exchange) {
        return BindingBuilder.bind(delayQueue).to(exchange).with(EXCHANGE_BINDING_DELAY_QUEUE_ROUTING_KEY).noargs();
    }


    @Bean
    public Binding stockReleaseBinding(@Qualifier("stockReleaseStockQueue") Queue deadQueue,
                                       @Qualifier("exchange") Exchange exchange) {
        return BindingBuilder.bind(deadQueue).to(exchange).with(EXCHANGE_BINDING_DEAD_QUEUE_ROUTING_KEY).noargs();
    }
}
