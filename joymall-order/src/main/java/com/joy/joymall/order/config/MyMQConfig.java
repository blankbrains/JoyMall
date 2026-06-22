package com.joy.joymall.order.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/1 21:59
 */
@Configuration
public class MyMQConfig {
    /**
     * 延时队列名称
     */
    public static final String DELAY_QUEUE = "order.delay.queue";
    /**
     * 死信队列名称
     */
    public static final String DEAD_QUEUE = "order.release.order.queue";
    /**
     * 交换机名称
     */
    public static final String EXCHANGE = "order-event-exchange";
    /**
     * 延时队列的消息通过交换机发送给死信队列的routing-key
     */
    public static final String EXCHANGE_ROUTING_DEAD_QUEUE_ROUTING_KEY = "order.release.order";
    /**
     * 延时队列中消息过期时间
     */
    public static final int DELAY_QUEUE_TTL = 1000 * 60;
    /**
     * 交换机绑定延时队列的routing-key
     */
    public static final String EXCHANGE_BINDING_DELAY_QUEUE_ROUTING_KEY = "order.create.order";
    /**
     * 交换机绑定死信队列的routing-key
     */
    public static final String EXCHANGE_BINDING_DEAD_QUEUE_ROUTING_KEY = "order.release.order";
    /**
     * 库存的死信队列
     */
    public static final String WARE_DEAD_QUEUE = "stock.release.stock.queue";
    /**
     * 订单释放返回交换机，交换机再路由到库存死信队列的Routing-key
     */
    public static final String ORDER_EXCHANGE_ROUTING_WARE_DEAD_QUEUE_ROUTING_KEY = "order.release.other.#";
    /**
     * 秒杀队列
     */
    public static final String SECKILL_QUEUE = "order.seckill.order.queue";
    /**
     * 当前交换机与秒杀队列绑定routing-key
     */
    public static final String EXCHANGE_BINDING_SECKILL_QUEUE_ROUTING_KEY = "order.seckill.order";

    /**
     * 创建延时队列
     * String name, boolean durable, boolean exclusive, boolean autoDelete, @Nullable Map<String, Object> arguments
     *
     * @return : 延时队列
     */
    @Bean("orderDelayQueue")
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>(3);
        //消息到时间路由到那个交换机
        args.put("x-dead-letter-exchange", EXCHANGE);
        //路由到交换机通过那个routing-key发送给队列
        args.put("x-dead-letter-routing-key", EXCHANGE_ROUTING_DEAD_QUEUE_ROUTING_KEY);
        //消息过期时间
        args.put("x-message-ttl", DELAY_QUEUE_TTL);

        return new Queue(DELAY_QUEUE, true, false, false, args);
    }

    /**
     * 创建死信队列
     *
     * @return ： 死信队列
     */
    @Bean("orderReleaseOrderQueue")
    public Queue orderReleaseOrderQueue() {

        return new Queue(DEAD_QUEUE, true, false, false);
    }

    /**
     * @return : 秒杀队列
     */
    @Bean("orderSecKillOrderQueue")
    public Queue orderSecKillOrderQueue() {
        return new Queue(SECKILL_QUEUE, true, false, false);
    }

    /**
     * 创建交换机
     *
     * @return ： 中转交换机
     */
    @Bean("orderEventExchange")
    public Exchange orderEventExchange() {

        return new TopicExchange(EXCHANGE, true, false);
    }

    /**
     * 延时队列绑定交换机
     * String destination, Binding.DestinationType destinationType, String exchange, String routingKey, @Nullable Map<String, Object> arguments
     * 通过创建Binding对象实现
     *
     * @return ： 绑定关系
     */
    @Bean
    public Binding orderCreateOrderBinding() {
        return new Binding(DELAY_QUEUE,
                Binding.DestinationType.QUEUE,
                EXCHANGE,
                EXCHANGE_BINDING_DELAY_QUEUE_ROUTING_KEY,
                null);
    }

    /**
     * 死信队列绑定交换机
     * <p>
     * 通过BindingBuilder实现
     *
     * @return ： 绑定关系
     */
    @Bean
    public Binding orderReleaseOrderBinding(@Qualifier("orderReleaseOrderQueue") Queue deadQueue,
                                            @Qualifier("orderEventExchange") Exchange exchange) {

        return BindingBuilder.bind(deadQueue).to(exchange).with(EXCHANGE_BINDING_DEAD_QUEUE_ROUTING_KEY).noargs();
    }


    /**
     * @return : 订单交换机绑定库存死信队列
     */
    @Bean
    public Binding orderExchangeBindingWareQueue() {
        return new Binding(WARE_DEAD_QUEUE, Binding.DestinationType.QUEUE, EXCHANGE, ORDER_EXCHANGE_ROUTING_WARE_DEAD_QUEUE_ROUTING_KEY, null);
    }


    /**
     * @param exchange               ： 订单交换机
     * @param orderSecKillOrderQueue ： 秒杀队列
     * @return ：
     */
    @Bean
    public Binding orderExchangeBindingSecKillQueue(@Qualifier("orderEventExchange") Exchange exchange,
                                                    @Qualifier("orderSecKillOrderQueue") Queue orderSecKillOrderQueue) {
        return BindingBuilder.bind(orderSecKillOrderQueue).to(exchange).with(EXCHANGE_BINDING_SECKILL_QUEUE_ROUTING_KEY).noargs();
    }

}
