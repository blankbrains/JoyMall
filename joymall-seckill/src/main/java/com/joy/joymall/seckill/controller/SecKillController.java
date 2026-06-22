package com.joy.joymall.seckill.controller;

import com.joy.common.utils.R;
import com.joy.joymall.seckill.service.SecKillService;
import com.joy.joymall.seckill.to.SecKillSkuRedisTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/13 23:05
 */
@Controller
public class SecKillController {

    @Resource
    private SecKillService secKillService;

    @ResponseBody
    @GetMapping("/getCurrentSecKillSuks")
    public R getCurrentSecKillSuks() {
        List<SecKillSkuRedisTo> skus = secKillService.getCurrentSecKillSuks();
        return R.ok().setData(skus);
    }


    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSecKillInfo(@PathVariable("skuId") Long skuId) {
        SecKillSkuRedisTo to = secKillService.getSkuSecKillInfo(skuId);

        return R.ok().setData(to);
    }


    @GetMapping("/kill")
    public String kill(@RequestParam("killId") String killId,
                       @RequestParam("key") String key,
                       @RequestParam("num") Long num,
                       Model model) {

        String orderSn = secKillService.kill(killId, key, num);

        model.addAttribute("orderSn", orderSn);
        return "success";
    }
}
