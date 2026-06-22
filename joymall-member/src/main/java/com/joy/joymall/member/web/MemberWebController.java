package com.joy.joymall.member.web;

import com.joy.common.utils.R;
import com.joy.joymall.member.feign.OrderFeignService;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/7 21:39
 */
@Controller
public class MemberWebController {

    @Resource
    private OrderFeignService orderFeignService;

    @GetMapping("/memberOrder.html")
    public String memberOrder(@RequestParam(value = "pageNum", defaultValue = "1") String pageNum,
                              Model model, HttpServletRequest request) {
        //通过request获得支付宝传来的数据

        //查出当前用户的所有订单列表数据
        Map<String, Object> params = new HashMap<>();
        params.put("page", pageNum);
        R r = orderFeignService.listWithItem(params);
        model.addAttribute("orders", r);
        return "orderList";
    }
}
