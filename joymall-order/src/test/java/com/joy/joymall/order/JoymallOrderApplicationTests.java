package com.joy.joymall.order;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;

@SpringBootTest
@Slf4j
class JoymallOrderApplicationTests {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    AmqpAdmin amqpAdmin;

    @Test
    void contextLoads() {
    }

    @Test
    void testExchange() {
        amqpAdmin.declareExchange(new DirectExchange("joymall.direct", true, false));
        amqpAdmin.declareExchange(ExchangeBuilder.directExchange("joymall.direct.builder")
                .durable(true).build());

        log.info("exchange successful!");
    }


    @Test
    void testQueueAndBinding() {
        amqpAdmin.declareQueue(new Queue("joymall.queue", true, false, false));
        log.info("队列创建成功");
        BindingBuilder.bind(new Queue("joymall.queue"))
                .to(new DirectExchange("joymall.direct")).with("direct.binding");
        log.info("交换机队列绑定成功");
    }


    @Test
    void testBinding() {
        amqpAdmin.declareBinding(new Binding("joymall.queue", Binding.DestinationType.QUEUE,
                "joymall.direct", "direct.binding", new HashMap<>()));
    }


    @Test
    void testSend(){

        rabbitTemplate.convertAndSend("joymall.direct","direct.binding","Hello Rabbit");
    }
}
