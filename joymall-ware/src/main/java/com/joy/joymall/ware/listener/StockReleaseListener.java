package com.joy.joymall.ware.listener;


import com.joy.common.to.OrderTo;

import com.joy.common.to.mq.StockLockedTo;

import com.joy.joymall.ware.config.MyRabbitConfig;

import com.joy.joymall.ware.service.WareSkuService;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/3 0:18
 */
@Service
@RabbitListener(queues = {MyRabbitConfig.DEAD_QUEUE})
public class StockReleaseListener {

    @Autowired
    private WareSkuService wareSkuService;

    public static final Logger log = LoggerFactory.getLogger(StockReleaseListener.class);

    /** 最大重试次数，超过则丢弃（进入死信队列或被丢弃） */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 获取消息的重试次数（从 x-death 头中解析）
     */
    private int getRetryCount(Message message) {
        List<Map<String, ?>> xDeathHeader = message.getMessageProperties().getXDeathHeader();
        if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
            Object countObj = xDeathHeader.get(0).get("count");
            if (countObj instanceof Long) {
                return ((Long) countObj).intValue();
            }
        }
        return 0;
    }

    /**
     * 下订单成功，库存也锁定成功，但由于其它业务调用（如扣积分之类）失败，导致订单回滚，订单锁定就要解锁
     * <p>
     * 只要收到消息，就会自动ack，可能会造成消息丢失，但是解锁失败
     *
     * @param lockedTo : 锁定的库存信息
     * @param channel  ： 一条消费者连接队列的通道
     */
    @RabbitHandler
    private void handleStockLockedRelease(StockLockedTo lockedTo, Channel channel, Message message) throws IOException {

        try {
            wareSkuService.releaseLockedStock(lockedTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("库存解锁成功");
        } catch (Exception e) {
            int retryCount = getRetryCount(message);
            if (retryCount >= MAX_RETRY_COUNT) {
                log.error("库存解锁消息重试{}次仍失败，已丢弃: lockedTo={}", retryCount, lockedTo, e);
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                log.warn("库存解锁消息第{}次处理失败，重新入队: {}", retryCount + 1, e.getMessage());
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
            }
        }
    }


    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderVo, Channel channel, Message message) throws IOException {
        try {
            wareSkuService.releaseLockedStock(orderVo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("订单:{}关闭，解锁库存", orderVo.getOrderSn());
        } catch (Exception e) {
            int retryCount = getRetryCount(message);
            if (retryCount >= MAX_RETRY_COUNT) {
                log.error("订单关闭解锁库存消息重试{}次仍失败，已丢弃: orderSn={}", retryCount, orderVo.getOrderSn(), e);
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                log.warn("订单关闭解锁库存第{}次处理失败，重新入队: {}", retryCount + 1, e.getMessage());
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
            }
        }
    }
}
