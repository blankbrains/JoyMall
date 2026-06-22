package com.joy.joymall.order.web;

import com.joy.joymall.order.service.OrderService;
import com.joy.joymall.order.vo.OrderConfirmVo;
import com.joy.joymall.order.vo.OrderSubmitRespVo;
import com.joy.joymall.order.vo.OrderSubmitVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/27 14:38
 */
@Api("订单页面")
@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;


    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.confirmOrder();

        model.addAttribute("orderConfirmData", confirmVo);
        return "confirm";
    }


    @ApiOperation("订单提交")
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo orderSubmitVo, Model model, RedirectAttributes redirectAttributes) {
        //下单：创建订单、验证token、验证价格、锁定库存
        //成功:支付页面
        //失败：重新回到confirm页面
        OrderSubmitRespVo respVo = orderService.submitOrder(orderSubmitVo);
        if (respVo.getCode() == 0) {
            model.addAttribute("orderRespVo", respVo);
            return "pay";
        }
        String msg = "";
        switch (respVo.getCode()) {
            case 1:
                msg = "订单信息过期，请重新提交";
                break;
            case 2:
                msg = "订单商品价格发生变化，请确认后提交";
                break;
            case 3:
                msg = "商品库存不足";
                break;
            default:
                msg = "未知错误，请联系管理员";
                break;
        }
        redirectAttributes.addFlashAttribute("msg", msg);
        return "redirect:http://order.joymall.com/toTrade";
    }
}
