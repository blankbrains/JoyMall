package com.joy.joymall.product.app;

import java.util.Arrays;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.joy.joymall.product.entity.CategoryEntity;
import com.joy.joymall.product.service.CategoryService;

import com.joy.common.utils.R;


/**
 * 商品三级分类
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-03 23:12:30
 */
@RestController
@RequestMapping("/product/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 查出所有分类以及子分类，以树形结构组装列表
     */
    @RequestMapping("/list/tree")
    public R list() {
        //一次性查询所有并组装成树形结构
        List<CategoryEntity> entities = categoryService.listWithTree();

        return R.ok().put("data", entities);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{catId}")
    public R info(@PathVariable("catId") Long catId) {
        CategoryEntity category = categoryService.getById(catId);

        return R.ok().put("data", category);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody CategoryEntity category) {
        categoryService.save(category);

        return R.ok();
    }

    /**
     * 批量更新并排序
     *
     * @param entities
     * @return
     */
    @RequestMapping("/update/sort")
    public R updateBatch(@RequestBody CategoryEntity[] entities) {

        categoryService.updateBatchById(Arrays.asList(entities));
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody CategoryEntity category) {
        //需要更新中间表

        categoryService.updateDetails(category);

        return R.ok();
    }

    /**
     * 删除
     * RequestBody:获取请求体，只有Post请求才有请求体
     * SpringMVC自动将请求体的JSON转为对象
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] catIds) {


        categoryService.removeMenuByIds(Arrays.asList(catIds));

        return R.ok();
    }

}
