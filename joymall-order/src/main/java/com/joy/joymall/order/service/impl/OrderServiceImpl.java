package com.joy.joymall.order.service.impl;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.joy.common.to.OrderTo;
import com.joy.common.to.mq.SecKillOrderTo;
import com.joy.common.utils.R;
import com.joy.common.vo.MemberResponseVo;
import com.joy.joymall.order.config.MyMQConfig;
import com.joy.joymall.order.constant.OrderConstant;
import com.joy.joymall.order.entity.OrderItemEntity;
import com.joy.joymall.order.entity.PaymentInfoEntity;
import com.joy.joymall.order.enume.OrderStatusEnum;
import com.joy.joymall.order.feign.CartFeignService;
import com.joy.joymall.order.feign.MemberFeignService;
import com.joy.joymall.order.feign.ProductFeignService;
import com.joy.joymall.order.feign.WareFeignService;
import com.joy.joymall.order.interceptor.LoginUserInterceptor;
import com.joy.joymall.order.service.OrderItemService;
import com.joy.joymall.order.service.PaymentInfoService;
import com.joy.joymall.order.to.OrderCreateTo;
import com.joy.joymall.order.vo.*;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.order.dao.OrderDao;
import com.joy.joymall.order.entity.OrderEntity;
import com.joy.joymall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.annotation.Resource;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    //共享订单数据
    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private OrderItemService orderItemService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private PaymentInfoService paymentInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginThreadLocal.get();

        //每一个异步都会开启一个线程，此时主线程的ThreadLocal就不会在异步线程生效，即数据消失了
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //会员地址 member服务
        CompletableFuture<Void> getAddr = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> memberAddress = memberFeignService.getAddress(memberResponseVo.getId());
            confirmVo.setAddress(memberAddress);
        }, executor);

        //购物车所有需要结账的购物单  cart服务
        //feign远程调用会丢失请求头，导致登录的用户呈现未登录状态
        //解决方法：加上自己的拦截器，在拦截器中加入请求头
        CompletableFuture<Void> getCartItem = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> currentCheckedCartItem = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(currentCheckedCartItem);
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            //根据商品id查询批量查询库存状态
            List<Long> skuIds = items.stream()
                    .map(OrderItemVo::getSkuId)
                    .collect(Collectors.toList());
            //1调用远程库存服务查询库存
            R hasStock = wareFeignService.getSkuHasStock(skuIds);
            List<SkuHasStockVo> skuHasStockVos = hasStock.getData(new TypeReference<List<SkuHasStockVo>>() {
            });
            if (skuHasStockVos != null) {
                Map<Long, Boolean> resultMap = skuHasStockVos.stream()
                        .collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::isHasStock));
                confirmVo.setHasStock(resultMap);
            }
        }, executor);


        //查询用户积分 当前用户
        CompletableFuture<Void> getIntegration = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            Integer integration = memberResponseVo.getIntegration();
            confirmVo.setIntegration(integration);
        }, executor);


        //其余数据自动计算

        //防重令牌，解决幂等性问题
        String uuid = UUID.randomUUID().toString().replace("-", "");
        //保存到页面，方便提交时带上校验
        confirmVo.setOrderToken(uuid);
        //保存到服务器，进行校验
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId().toString(), uuid, 30, TimeUnit.MINUTES);

        CompletableFuture.allOf(getAddr, getCartItem, getIntegration).get();
        return confirmVo;
    }


    /**
     * 下单：创建订单、验证token、验证价格、锁定库存
     *
     * @param orderSubmitVo :提交来的订单数据
     * @return ：提交成功后返回结果
     */
//    @GlobalTransactional
    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitRespVo submitOrder(OrderSubmitVo orderSubmitVo) {
        confirmVoThreadLocal.set(orderSubmitVo);
        OrderSubmitRespVo respVo = new OrderSubmitRespVo();
        respVo.setCode(0);
        //获取登录用户
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginThreadLocal.get();
        //获取用户传来的令牌
        String orderToken = orderSubmitVo.getOrderToken();
        //令牌对比删除lua脚本
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        /**
         * 原子验证和删除令牌
         * 参数1：脚本和返回值类型：0失败，1成功
         * 参数2：要获取的key值
         * 参数3：要进行对比的值
         */
        Long res = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()), orderToken);
        if (res != null && res.equals(1L)) {
            //验证成功，创建订单、验证价格、锁定库存
            //1.创建订单
            OrderCreateTo order = createOrder();
            //2.验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = orderSubmitVo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 10) {
                //3.保存订单到数据库
                saveOrder(order);
                //4.锁定库存,有异常回滚数据，通过MQ发送错误消息异步回滚，保证高并发，保证最终一致性
                //需要的数据：订单号，订单项（skuId，skuName，num）
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> lockVo = order.getOrderItems()
                        .stream()
                        .map((item) -> {
                            OrderItemVo vo = new OrderItemVo();
                            vo.setSkuId(item.getSkuId());
                            vo.setCount(item.getSkuQuantity());
                            vo.setTitle(item.getSkuName());
                            return vo;
                        }).collect(Collectors.toList());
                wareSkuLockVo.setLockVos(lockVo);
                R r = wareFeignService.orderLockStock(wareSkuLockVo);
                if (r.getCode() == 0) {
                    //锁定成功
                    respVo.setOrderEntity(order.getOrder());
                    //订单创建成功，发送消息给mq
                    rabbitTemplate.convertAndSend(MyMQConfig.EXCHANGE, MyMQConfig.EXCHANGE_BINDING_DELAY_QUEUE_ROUTING_KEY, order.getOrder());
                    return respVo;
                } else {
                    //锁定失败
                    respVo.setCode(3);
                    return respVo;
                }
            } else {
                respVo.setCode(2);
            }
        }
        return respVo;
    }

    @Override
    public OrderEntity getOrderStatus(String orderSn) {
        return this.getOne(new QueryWrapper<OrderEntity>().lambda().eq(OrderEntity::getOrderSn, orderSn));
    }

    @Override
    public void releaseOrder(OrderEntity orderEntity) {
        //关闭订单之前先查询当前订单状态
        //需要再次查询一下数据库，因为当下订单发送给mq消息时的订单状态是未支付状态，在30分钟内可能会支付完成
        OrderEntity newestOrder = this.baseMapper.selectById(orderEntity.getId());
        if (newestOrder.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())) {
            //关闭订单
            OrderEntity relaseEntity = new OrderEntity();
            relaseEntity.setStatus(OrderStatusEnum.CANCLED.getCode());
            relaseEntity.setId(newestOrder.getId());
            this.updateById(relaseEntity);
            //关闭订单后再发送消息给库存死信队列
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(newestOrder, orderTo);
            try {
                //TODO 保证消息因为网络问题不会丢失
                //每一个发送的消息，都要进行日志记录
                rabbitTemplate.convertAndSend(MyMQConfig.EXCHANGE, MyMQConfig.ORDER_EXCHANGE_ROUTING_WARE_DEAD_QUEUE_ROUTING_KEY, orderTo);
            } catch (Exception e) {

            }
        }
    }

    /**
     * 获取订单支付信息
     *
     * @param orderSn
     * @return
     */
    @Override
    public PayVo getPayInfo(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity orderStatus = this.getOrderStatus(orderSn);
        payVo.setTotal_amount(orderStatus.getPayAmount().setScale(2, BigDecimal.ROUND_UP).toString());
        payVo.setOut_trade_no(orderSn);
        //给一个订单项信息
        List<OrderItemEntity> itemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().lambda().eq(OrderItemEntity::getOrderSn, orderSn));
        if (!CollectionUtils.isEmpty(itemEntities)) {
            payVo.setSubject(itemEntities.get(0).getSkuName());
            payVo.setBody(itemEntities.get(0).getSpuName());
        }
        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginThreadLocal.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().lambda()
                        .eq(OrderEntity::getMemberId, memberResponseVo.getId())
                        .orderByDesc(OrderEntity::getId)
        );
        //查询出所有订单项并设置
        List<OrderEntity> orderEntities = page.getRecords().stream().peek(orderEntity -> {
            List<OrderItemEntity> orderItemEntities = orderItemService.list(new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderSn, orderEntity.getOrderSn()));
            orderEntity.setOrderItemEntities(orderItemEntities);
        }).collect(Collectors.toList());
        //重新填入数据
        page.setRecords(orderEntities);
        return new PageUtils(page);
    }


    /**
     * 处理订单状态
     *
     * @param payAsyncVo ： 支付宝返回的数据
     * @return ：
     */
    @Override
    public String handlePayResult(PayAsyncVo payAsyncVo) {
        //保存交易流水到数据库
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setAlipayTradeNo(payAsyncVo.getTrade_no());
        paymentInfoEntity.setOrderSn(payAsyncVo.getOut_trade_no());
        paymentInfoEntity.setPaymentStatus(payAsyncVo.getTrade_status());
        paymentInfoEntity.setCallbackTime(payAsyncVo.getNotify_time());
        paymentInfoService.save(paymentInfoEntity);
        //交易成功
        if ("TRADE_SUCCESS".equals(payAsyncVo.getTrade_status()) || "TRADE_FINISHED".equals(payAsyncVo.getTrade_status())) {
            //修改订单状态
            String orderSn = payAsyncVo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode());
        }
        return "success";

    }

    @Override
    public void createSecKillOrder(SecKillOrderTo secKillOrderTo) {
        //TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(secKillOrderTo.getOrderSn());
        BigDecimal payAmount = secKillOrderTo.getSeckillPrice().multiply(new BigDecimal(secKillOrderTo.getNum()));
        orderEntity.setPayAmount(payAmount);
        orderEntity.setMemberId(secKillOrderTo.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        this.baseMapper.insert(orderEntity);

        //TODO 保存订单项信息
        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setOrderSn(secKillOrderTo.getOrderSn());
        itemEntity.setRealAmount(payAmount);
        itemEntity.setSkuQuantity(secKillOrderTo.getNum());
        orderItemService.save(itemEntity);
    }


    /**
     * 订单保存到数据库
     *
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }


    /**
     * 构建总订单
     *
     * @return
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //1.订单信息
        //生成订单号
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrderEntity(orderSn);
        orderCreateTo.setOrder(orderEntity);
        //2.订单项信息
        List<OrderItemVo> userCartItems = cartFeignService.getCurrentUserCartItems();
        List<OrderItemEntity> orderItemEntities = buildOrderItemEntities(userCartItems, orderSn);
        orderCreateTo.setOrderItems(orderItemEntities);
        //3.验价
        couputePrice(orderEntity, orderItemEntities);
        return orderCreateTo;
    }

    private void couputePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        BigDecimal totalPrice = new BigDecimal(0);
        BigDecimal totalCouponAmount = new BigDecimal(0);
        BigDecimal totalIntegrationAmount = new BigDecimal(0);
        BigDecimal totalPromotionAmount = new BigDecimal(0);
        BigDecimal totalGift = new BigDecimal(0);
        BigDecimal totalGrowth = new BigDecimal(0);
        //订单总额 = 叠加的订单项总额
        if (!CollectionUtils.isEmpty(orderItemEntities)) {
            for (OrderItemEntity orderItemEntity : orderItemEntities) {
                //应付总额，优惠总额，积分总额，促销总额,积分信息，成长值信息
                BigDecimal realAmount = orderItemEntity.getRealAmount();
                totalPrice = totalPrice.add(realAmount);
                BigDecimal couponAmount = orderItemEntity.getCouponAmount();
                totalCouponAmount = totalCouponAmount.add(couponAmount);
                BigDecimal integrationAmount = orderEntity.getIntegrationAmount();
                if (totalIntegrationAmount != null && integrationAmount != null) {
                    totalIntegrationAmount = totalIntegrationAmount.add(integrationAmount);
                }

                BigDecimal promotionAmount = orderEntity.getPromotionAmount();
                if (promotionAmount != null && totalPromotionAmount != null) {
                    totalPromotionAmount = totalPromotionAmount.add(promotionAmount);
                }
                Integer growth = orderItemEntity.getGiftGrowth();
                totalGrowth = totalGrowth.add(new BigDecimal(growth));
                Integer giftIntegration = orderItemEntity.getGiftIntegration();
                totalGift = totalGift.add(new BigDecimal(giftIntegration));
            }
        }
        //订单总额
        orderEntity.setTotalAmount(totalPrice);
        //应付总额（含运费）
        orderEntity.setPayAmount(totalPrice.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(totalPromotionAmount);
        orderEntity.setIntegrationAmount(totalIntegrationAmount);
        orderEntity.setCouponAmount(totalCouponAmount);
        orderEntity.setGrowth(totalGrowth.intValue());
        orderEntity.setIntegration(totalGift.intValue());
    }


    /**
     * 构建订单
     *
     * @param orderSn
     * @return
     */
    private OrderEntity buildOrderEntity(String orderSn) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginThreadLocal.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberId(memberResponseVo.getId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        //获取收货地址信息
        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();
        Long addrId = orderSubmitVo.getAddrId();
        if (ObjectUtils.isEmpty(addrId)) {
            orderSubmitVo.setAddrId(1L);
            addrId = 1L;
        }
        R fare = wareFeignService.getFreight(addrId);
        FareVo fareData = fare.getData(new TypeReference<FareVo>() {
        });
        //设置运费
        orderEntity.setFreightAmount(fareData.getFare());
        //设置收货信息
        if (fareData.getMemberAddressVo() != null) {
            if (fareData.getMemberAddressVo().getCity() != null) {
                orderEntity.setReceiverCity(fareData.getMemberAddressVo().getCity());
            }
            if (fareData.getMemberAddressVo().getDetailAddress() != null) {
                orderEntity.setReceiverDetailAddress(fareData.getMemberAddressVo().getDetailAddress());
            }
            orderEntity.setReceiverName(fareData.getMemberAddressVo().getName());
            orderEntity.setReceiverPhone(fareData.getMemberAddressVo().getPhone());
            orderEntity.setReceiverPostCode(fareData.getMemberAddressVo().getPostCode());
            orderEntity.setReceiverProvince(fareData.getMemberAddressVo().getProvince());
            orderEntity.setReceiverRegion(fareData.getMemberAddressVo().getRegion());
        }
        //订单状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setConfirmStatus(14);
        orderEntity.setDeleteStatus(0);
        return orderEntity;
    }


    /**
     * 由订单项vo构建订单项entity
     *
     * @param orderItemVos
     * @return
     */
    private List<OrderItemEntity> buildOrderItemEntities(List<OrderItemVo> orderItemVos, String orderSn) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        if (!CollectionUtils.isEmpty(orderItemVos)) {
            orderItemEntities = orderItemVos.stream().map(cartItem -> {
                OrderItemEntity orderItemEntity = buildOrderItemEntity(cartItem);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
        }
        return orderItemEntities;
    }


    /**
     * 构建单个订单项
     *
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItemEntity(OrderItemVo cartItem) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //1.订单信息
        //2.sku信息
        orderItemEntity.setSkuId(cartItem.getSkuId());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttr);
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImage());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        orderItemEntity.setSkuQuantity(cartItem.getCount());
        //3.spu信息
        Long skuId = cartItem.getSkuId();
        //通过skuId查到spu信息
        R getSpuBySkuId = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoVo = getSpuBySkuId.getData(new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(spuInfoVo.getId());
        orderItemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
        orderItemEntity.setSpuName(spuInfoVo.getSpuName());
        orderItemEntity.setSpuPic(cartItem.getImage());
        orderItemEntity.setCategoryId(spuInfoVo.getCatalogId());
        //4.优惠信息（忽略）
        //5.积分信息
        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue() / 10);
        //6.设置真实价格信息（优惠后的）
        BigDecimal promotionAmount = new BigDecimal("0.0");
        orderItemEntity.setPromotionAmount(promotionAmount);
        BigDecimal couponAmount = new BigDecimal("0.0");
        orderItemEntity.setCouponAmount(couponAmount);
        BigDecimal integrationAmount = new BigDecimal("0.0");
        orderItemEntity.setIntegrationAmount(integrationAmount);
        //当前订单项的实际金额
        BigDecimal origin = cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount()));
        BigDecimal finalPrice = origin.subtract(promotionAmount)
                .subtract(couponAmount)
                .subtract(integrationAmount);
        orderItemEntity.setRealAmount(finalPrice);
        return orderItemEntity;
    }
}