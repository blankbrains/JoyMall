package com.joy.joymall.cart.service;

import com.joy.joymall.cart.vo.CartItemVo;
import com.joy.joymall.cart.vo.CartVo;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/9 17:01
 */
public interface CartService {

    CartItemVo addToCart(Long skuId, Long num) throws ExecutionException, InterruptedException;

    CartItemVo getCartItem(Long skuId);

    CartVo getCart() throws ExecutionException, InterruptedException;

    void clearCart(String cartKey);

    void checkItem(Long skuId, Integer check);

    void countItem(Long skuId, Integer count);

    void deleteItem(Long skuId);

    List<CartItemVo> getCurrentUserCartItems();

}
