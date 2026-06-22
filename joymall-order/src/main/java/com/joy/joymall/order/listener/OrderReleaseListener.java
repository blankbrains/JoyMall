package com.joy.joymall.order.listener;

import com.joy.joymall.order.config.MyMQConfig;
import com.joy.joymall.order.entity.OrderEntity;
import com.joy.joymall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/3 19:49
 */
@RabbitListener(queues = {MyMQConfig.DEAD_QUEUE})
@Service
public class OrderReleaseListener {

    @Autowired
    private OrderService orderService;

    public static final Logger log = LoggerFactory.getLogger(OrderReleaseListener.class);


    @RabbitHandler
    public void releaseOrder(OrderEntity orderEntity, Channel channel, Message message) throws IOException {
        try {
            orderService.releaseOrder(orderEntity);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            log.info("订单:{}关闭成功",orderEntity.getOrderSn());
        }catch (Exception e){
            log.error("关闭订单:{}失败",orderEntity.getOrderSn());
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }

}
