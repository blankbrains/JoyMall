package com.joy.joymall.order.web;

import cn.hutool.core.lang.UUID;
import com.joy.joymall.order.config.MyMQConfig;
import com.joy.joymall.order.entity.OrderEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/16 0:36
 */
@Controller
public class HelloController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/confirm.html")
    public String confirm() {

        return "confirm";
    }

    @GetMapping("/detail.html")
    public String detail() {

        return "detail";
    }


    @GetMapping("/list.html")
    public String list() {

        return "list";
    }

    @GetMapping("/pay.html")
    public String pay() {

        return "pay";
    }


    @ResponseBody
    @GetMapping("/test/createOrder")
    public String createOrderTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(UUID.randomUUID().toString());
        orderEntity.setModifyTime(new Date());
        //给MQ发送消息
        rabbitTemplate.convertAndSend(MyMQConfig.EXCHANGE, MyMQConfig.EXCHANGE_BINDING_DELAY_QUEUE_ROUTING_KEY, orderEntity);
        return "success";
    }
}
