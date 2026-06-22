package com.joy.joymall.order.web;

import com.alipay.api.AlipayApiException;
import com.joy.joymall.order.config.AlipayTemplate;
import com.joy.joymall.order.service.OrderService;
import com.joy.joymall.order.vo.PayVo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/7 20:38
 */
@Controller
public class PayWebController {

    @Resource
    private AlipayTemplate alipayTemplate;

    @Resource
    private OrderService orderService;

    @GetMapping(value = "/payOrder",produces = "text/html")
    @ResponseBody
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {

        PayVo payVo = orderService.getPayInfo(orderSn);
        String pay = alipayTemplate.pay(payVo);
        return pay;
    }
}
