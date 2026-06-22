package com.joy.joymall.product.web;

import com.joy.joymall.product.entity.CategoryEntity;
import com.joy.joymall.product.service.CategoryService;
import com.joy.joymall.product.vo.Category2Vo;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;


/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/25 20:47
 */
@Controller
public class IndexController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model) {

        //TODO 查询所有的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getOneLevels();

        model.addAttribute("categorys", categoryEntities);
        return "index";
    }

    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Category2Vo>> getCatalogJson() {
        Map<String, List<Category2Vo>> catalogJson = categoryService.getCatalogJson();

        return catalogJson;
    }

}
