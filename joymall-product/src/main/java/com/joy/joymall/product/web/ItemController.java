package com.joy.joymall.product.web;

import com.joy.joymall.product.entity.SkuInfoEntity;
import com.joy.joymall.product.service.SkuInfoService;
import com.joy.joymall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.ExecutionException;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/2 23:44
 */
@Controller
public class ItemController {

    @Autowired
    private SkuInfoService skuInfoService;

    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId, Model model) {

        SkuItemVo skuItemVo = null;
        try {
            //异步编排可能异常
            skuItemVo = skuInfoService.item(skuId);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        model.addAttribute("item", skuItemVo);
        return "item";
    }
}
