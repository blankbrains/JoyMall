package com.joy.joymall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.joy.common.to.OrderTo;
import com.joy.common.to.mq.SecKillOrderTo;
import com.joy.common.utils.PageUtils;
import com.joy.joymall.order.entity.OrderEntity;
import com.joy.joymall.order.vo.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-04 18:23:01
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    OrderSubmitRespVo submitOrder(OrderSubmitVo orderSubmitVo);

    OrderEntity getOrderStatus(String orderSn);

    void releaseOrder(OrderEntity orderEntity);

    PayVo getPayInfo(String orderSn);

    PageUtils queryPageWithItem(Map<String, Object> params);

    String handlePayResult(PayAsyncVo payAsyncVo);

    void createSecKillOrder(SecKillOrderTo secKillOrderTo);
}

