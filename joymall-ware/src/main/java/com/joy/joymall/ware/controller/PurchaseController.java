package com.joy.joymall.ware.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.joy.joymall.ware.vo.MergeVo;
import com.joy.joymall.ware.vo.purchasevo.PurchaseCompleteVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.joy.joymall.ware.entity.PurchaseEntity;
import com.joy.joymall.ware.service.PurchaseService;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.R;


/**
 * 采购信息
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-04 18:29:13
 */
@RestController
@RequestMapping("ware/purchase")
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;


    @PostMapping("/done")
    public R completePurchase(@RequestBody PurchaseCompleteVo completeVo) {

        purchaseService.done(completeVo);
        return R.ok();
    }


    /**
     * 领取采购单
     *
     * @param purchaseIds
     * @return
     */
    @PostMapping("/received")
    public R received(@RequestBody List<Long> purchaseIds) {

        purchaseService.received(purchaseIds);

        return R.ok();
    }


    /**
     * 将多个采购需求合并成一个采购单
     *
     * @param mergeVo
     * @return
     */
    @PostMapping("/merge")
    public R merge(@RequestBody MergeVo mergeVo) {
        purchaseService.mergePurchase(mergeVo);
        return R.ok();
    }

    /**
     * 查询所有刚新建的采购需求
     *
     * @param params
     * @return
     */
    @RequestMapping("/unreceive/list")
    public R unreceiveList(@RequestParam Map<String, Object> params) {
        PageUtils page = purchaseService.queryPageUnreceived(params);

        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = purchaseService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        PurchaseEntity purchase = purchaseService.getById(id);

        return R.ok().put("purchase", purchase);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody PurchaseEntity purchase) {

        purchase.setUpdateTime(new Date());
        purchase.setCreateTime(new Date());
        purchaseService.save(purchase);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody PurchaseEntity purchase) {
        purchase.setUpdateTime(new Date());
        purchaseService.updateById(purchase);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        purchaseService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
