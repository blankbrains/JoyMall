package com.joy.joymall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.joy.common.utils.R;
import com.joy.joymall.cart.feign.ProductFeignService;
import com.joy.joymall.cart.interceptor.CartInterceptor;
import com.joy.joymall.cart.service.CartService;
import com.joy.joymall.cart.vo.CartItemVo;
import com.joy.joymall.cart.vo.CartVo;
import com.joy.joymall.cart.vo.SkuInfoVo;
import com.joy.joymall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/9 17:01
 */

@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String CART_PREFIX = "joymall:cart:";


    @Autowired
    private ThreadPoolExecutor executor;


    @Autowired
    private ProductFeignService productFeignService;


    @Override
    public CartItemVo addToCart(Long skuId, Long num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String redisCartItem = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(redisCartItem)) {
            CartItemVo cartItemVo = new CartItemVo();
            //购物车为空
            //异步调用封装两个远程信息
            //新商品添加到购物车
            CompletableFuture<Void> baseSkuInfo = CompletableFuture.runAsync(() -> {
                //1.异步查询sku基本信息
                //远程调用查询当前sku的信息
                R r = productFeignService.getSkuinfo(skuId);
                SkuInfoVo skuInfo = r.getData(new TypeReference<SkuInfoVo>() {
                }, "skuInfo");
                cartItemVo.setCheck(true);
                cartItemVo.setCount(Math.toIntExact(num));
                cartItemVo.setImage(skuInfo.getSkuDefaultImg());
                cartItemVo.setPrice(skuInfo.getPrice());
                cartItemVo.setSkuId(skuId);
                cartItemVo.setTitle(skuInfo.getSkuTitle());
                cartItemVo.setTotalPrice();
            }, executor);

            CompletableFuture<Void> baseSkuAttr = CompletableFuture.runAsync(() -> {
                //2.异步查询sku属性信息
                List<String> saleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItemVo.setSkuAttr(saleAttrValues);
            }, executor);

            CompletableFuture<Void> finalFuture = CompletableFuture.allOf(baseSkuAttr, baseSkuInfo).thenRunAsync(() -> {
                //传对象会用jdk的序列化，不如直接传json，接收到再还原
                String jsonCartItem = JSON.toJSONString(cartItemVo);
                cartOps.put(skuId.toString(), jsonCartItem);
            });
            finalFuture.get();
            return cartItemVo;
        } else {
            //购物车中有此商品，更新一下vo
            CartItemVo cartItemVo = JSON.parseObject(redisCartItem, CartItemVo.class);
            cartItemVo.setCount(cartItemVo.getCount() + Math.toIntExact(num));
            cartItemVo.setTotalPrice();
            //再更新一下redis
            String newJsonCartItem = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), newJsonCartItem);
            return cartItemVo;
        }
    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String cartItemVo = (String) cartOps.get(skuId.toString());

        return JSON.parseObject(cartItemVo, CartItemVo.class);
    }

    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {
        CartVo cartVo = new CartVo();
        //1.首先需要判断登没登陆
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null && userInfoTo.getUserId() != 0L) {
            //登录了
            //1.先判断临时购物车有没有数据
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItemVo> tempCartItemVos = getCartItemVos(tempCartKey);
            //2.如果临时购物车有数据要进行合并，合并完成后清空临时购物车
            // 优化：直接从 Redis 读取临时购物车数据写入用户购物车，避免每次 addToCart 产生远程调用
            if (!CollectionUtils.isEmpty(tempCartItemVos)) {
                String cartKey = CART_PREFIX + userInfoTo.getUserId();
                for (CartItemVo tempCartItemVo : tempCartItemVos) {
                    // 检查用户购物车中是否已存在该商品，存在则累加数量
                    String skuKey = tempCartItemVo.getSkuId().toString();
                    String cartItemJson = (String) redisTemplate.opsForHash().get(cartKey, skuKey);
                    if (cartItemJson != null) {
                        CartItemVo existingItem = JSON.parseObject(cartItemJson, CartItemVo.class);
                        existingItem.setCount(existingItem.getCount() + tempCartItemVo.getCount());
                        redisTemplate.opsForHash().put(cartKey, skuKey, JSON.toJSONString(existingItem));
                    } else {
                        redisTemplate.opsForHash().put(cartKey, skuKey, JSON.toJSONString(tempCartItemVo));
                    }
                }
                //合并完成后删除临时购物车
                clearCart(tempCartKey);
            }

            //3.两种购物车都做好之后，再获取登录后的购物车数据
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItemVo> cartItemVos = getCartItemVos(cartKey);
            cartVo.setItems(cartItemVos);

        } else {
            //未登录
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            //获取临时购物车的所有购物项
            List<CartItemVo> cartItemVos = getCartItemVos(cartKey);
            cartVo.setItems(cartItemVos);
        }
        return cartVo;
    }

    /**
     * 清空购物车
     *
     * @param cartKey
     */
    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }


    /**
     * 更改购物项状态
     *
     * @param skuId
     * @param check
     */
    @Override
    public void checkItem(Long skuId, Integer check) {
        //更改状态
        CartItemVo cartItemVo = getCartItem(skuId);
        cartItemVo.setCheck(check == 1);

        //再次存入redis
        String jsonCartItem = JSON.toJSONString(cartItemVo);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), jsonCartItem);
    }

    @Override
    public void countItem(Long skuId, Integer count) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        //更改商品数量,如果为0就删除了
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCount(count);

        //保存回redis
        String jsonCartItem = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), jsonCartItem);
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        cartOps.delete(skuId.toString());
    }


    /**
     * 获取当前用户所有选中的购物项
     * @return
     */
    @Override
    public List<CartItemVo> getCurrentUserCartItems() {
        //获取当前登录的用户
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            //获取所有背选中的购物项
            List<CartItemVo> cartItemVos = getCartItemVos(cartKey).stream()
                    .filter(CartItemVo::getCheck)
                    .peek((item) -> {
                        //将购物车价格更新为最新价格
                        //TODO 优化点：可以一次查询完，然后逐个更新，避免远程调用浪费时间
                        BigDecimal currentPrice = productFeignService.getCurrentPrice(item.getSkuId());
                        item.setPrice(currentPrice);
                    })
                    .collect(Collectors.toList());
            return cartItemVos;
        }
    }

    /**
     * 获取某个key的购物车数据
     *
     * @param cartKey
     * @return
     */
    private List<CartItemVo> getCartItemVos(String cartKey) {
        BoundHashOperations<String, Object, Object> allCartItem = redisTemplate.boundHashOps(cartKey);
        List<Object> cartItems = allCartItem.values();
        List<CartItemVo> cartItemVos = null;
        if (!CollectionUtils.isEmpty(cartItems)) {
            cartItemVos = cartItems.stream().map((cartItemVo) -> {
                return JSON.parseObject((String) cartItemVo, CartItemVo.class);
            }).collect(Collectors.toList());
        }
        return cartItemVos;
    }


    /**
     * 获取到要操作的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        //判断用户是否登录
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null && userInfoTo.getUserId() != 0L) {
            //登录了
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            //没登录
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        //看看当前商品是否加入过购物车
        return redisTemplate.boundHashOps(cartKey);
    }
}
