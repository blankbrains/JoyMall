package com.joy.joymall.order.listener;

import com.joy.common.to.mq.SecKillOrderTo;
import com.joy.joymall.order.config.MyMQConfig;
import com.joy.joymall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/16 22:46
 */
@Component
@RabbitListener(queues = {MyMQConfig.SECKILL_QUEUE})
@Slf4j
public class OrderSecKillListener {
    @Autowired
    private OrderService orderService;


    @RabbitHandler
    public void releaseOrder(SecKillOrderTo secKillOrderTo, Channel channel, Message message) throws IOException {
        try {
            log.info("准备创建秒杀单的详细信息。。。");
            orderService.createSecKillOrder(secKillOrderTo);

        } catch (Exception e) {

        }
    }
}
