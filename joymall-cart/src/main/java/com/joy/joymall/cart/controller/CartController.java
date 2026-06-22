package com.joy.joymall.cart.controller;


import com.joy.joymall.cart.service.CartService;
import com.joy.joymall.cart.vo.CartItemVo;
import com.joy.joymall.cart.vo.CartVo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/9 17:07
 */
@Controller
public class CartController {

    @Autowired
    private CartService cartService;


    @ResponseBody
    @GetMapping("/currentUserCartItems")
    public List<CartItemVo> getCurrentUserCartItems() {

        return cartService.getCurrentUserCartItems();
    }

    /**
     * 删除购物车中某个商品
     *
     * @param skuId
     * @return
     */
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);

        return "redirect:http://cart.joymall.com/cart.html";
    }

    /**
     * 更改商品数量
     *
     * @param skuId
     * @param count
     * @return
     */
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("count") Integer count) {

        cartService.countItem(skuId, count);
        return "redirect:http://cart.joymall.com/cart.html";
    }


    /**
     * 更改购物车商品选中状态
     *
     * @param skuId
     * @param check
     * @return
     */
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check) {
        cartService.checkItem(skuId, check);

        return "redirect:http://cart.joymall.com/cart.html";
    }

    /**
     * 临时购物车的设计：在cookie中保存一个user-key，默认一个月后过期
     *
     * @param
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        CartVo cartVo = cartService.getCart();

        model.addAttribute("cart", cartVo);
        return "cartList";
    }


    /**
     * @param skuId
     * @param num
     * @param attributes : attributes.addFlashAttribute():将数据放在session中，只能取一次
     *                   attributes.addAttribute()：将数据拼在url后
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Long num,
                            RedirectAttributes attributes) throws ExecutionException, InterruptedException {
        cartService.addToCart(skuId, num);

        attributes.addAttribute("skuId", skuId);
        //直接用当前页面展示数据会导致重复刷新提交问题，重定向就好了
        return "redirect:http://cart.joymall.com/success";
    }


    /**
     * 跳转到成功页
     *
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/success")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId,
                                       Model model) {
        //解决重复请求问题，重定向到成功页面，再次查询数据即可
        CartItemVo cartItemVo = cartService.getCartItem(skuId);
        model.addAttribute("item", cartItemVo);
        return "success";
    }
}
