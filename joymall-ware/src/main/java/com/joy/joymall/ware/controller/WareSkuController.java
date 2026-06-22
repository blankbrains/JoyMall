package com.joy.joymall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.joy.common.exception.BizCodeEnum;


import com.joy.common.exception.NoStockException;
import com.joy.joymall.ware.vo.SkuHasStockVo;
import com.joy.joymall.ware.vo.WareSkuLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.joy.joymall.ware.entity.WareSkuEntity;
import com.joy.joymall.ware.service.WareSkuService;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.R;


/**
 * 商品库存
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-04 18:29:13
 */
@RestController
@RequestMapping("/ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;


    @PostMapping("/lock")
    public R orderLockStock(@RequestBody WareSkuLockVo wareSkuLockVo) {
        try {
            boolean respResult = wareSkuService.orderLockStock(wareSkuLockVo);
            return R.ok().setData(respResult);
        } catch (NoStockException e) {
            return R.error(BizCodeEnum.NO_STOCK_EXCEPTION.getCode(), BizCodeEnum.NO_STOCK_EXCEPTION.getMsg());
        }
    }


    /**
     * 查询Sku是否有库存
     */
    @PostMapping("/hasstock")
    public R getSkuHasStock(@RequestBody List<Long> skuIds) {

        List<SkuHasStockVo> hasStockVos = wareSkuService.getSkuHasStock(skuIds);

        return R.ok().setData(hasStockVos);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = wareSkuService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody WareSkuEntity wareSku) {
        wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody WareSkuEntity wareSku) {
        wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
