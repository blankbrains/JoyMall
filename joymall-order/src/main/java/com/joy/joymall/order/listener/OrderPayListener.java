package com.joy.joymall.order.listener;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.joy.joymall.order.config.AlipayTemplate;
import com.joy.joymall.order.service.OrderService;
import com.joy.joymall.order.vo.PayAsyncVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/7 23:46
 */
@Slf4j
@RestController
public class OrderPayListener {

    @Resource
    private OrderService orderService;

    @Resource
    private AlipayTemplate alipayTemplate;

    @GetMapping("/payed/notify")
    public String handleAlipayed(PayAsyncVo payAsyncVo, HttpServletRequest request) {
        //只要收到了支付宝的异步通知就响应success让支付宝不再通知,并修改订单状态
        //此异步回调是在支付完成但是还未跳转时触发的

        //验证签名，保证是正常支付，而不是恶意篡改
        Map<String, String> params = getRequestParams(request);
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(params,
                    alipayTemplate.getAlipay_public_key(),
                    alipayTemplate.getCharset(),
                    alipayTemplate.getSign_type());
            if (!signVerified) {
                log.error("支付宝异步通知签名验证失败: params={}", params);
                return "failure";
            }
        } catch (AlipayApiException e) {
            log.error("支付宝异步通知验签异常", e);
            return "failure";
        }

        String res = orderService.handlePayResult(payAsyncVo);
        return res;
    }

    private Map<String, String> getRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = iter.next();
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        return params;
    }
}
