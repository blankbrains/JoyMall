package com.joy.joymall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.joy.joymall.product.entity.AttrEntity;
import com.joy.joymall.product.service.AttrAttrgroupRelationService;
import com.joy.joymall.product.service.AttrService;
import com.joy.joymall.product.service.CategoryService;
import com.joy.joymall.product.vo.AttrGroupRelationVo;
import com.joy.joymall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.joy.joymall.product.entity.AttrGroupEntity;
import com.joy.joymall.product.service.AttrGroupService;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.R;


/**
 * 属性分组
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-03 23:12:30
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private AttrAttrgroupRelationService attrAttrgroupRelationService;


    /**
     * /product/attrgroup/{catelogId}/withattr
     * 获取当前分类下的所有属性分组的所有属性（比如手机下有颜色属性分组，cpu型号属性分组等等）
     */

    @GetMapping("/{catelogId}/withattr")
    public R getAttrGroupWithAttrs(@PathVariable("catelogId") Long catelogId) {
        //1.查出当前分类下的所有属性分组
        List<AttrGroupWithAttrsVo> attrsVos = attrGroupService.getAttrGroupWithAttrsByCatelogId(catelogId);
        //2.查出所有属性分组的所有属性

        return R.ok().put("data",attrsVos);
    }

    /**
     * 新增关联关系
     * /product/attrgroup/attr/relation
     *
     * @param attrGroupRelationVos
     * @return
     */

    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVo> attrGroupRelationVos) {
        attrAttrgroupRelationService.saveBatch(attrGroupRelationVos);

        return R.ok();
    }

    /**
     * 查询所有关联关系（一个属性组->多个属性）
     *
     * @param attrgroupId
     * @return
     */
    @GetMapping("/{attrgroupId}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupId") Long attrgroupId) {
        List<AttrEntity> entities = attrService.getRelationAttr(attrgroupId);

        return R.ok().put("data", entities);
    }

    /**
     * 查询所有没有的关联关系（一个属性组->多个属性）
     * <p>
     * /product/attrgroup/{attrgroupId}/noattr/relation
     *
     * @param attrgroupId
     * @return
     */
    @GetMapping("/{attrgroupId}/noattr/relation")
    public R attrNoRelation(@PathVariable("attrgroupId") Long attrgroupId,
                            @RequestParam Map<String, Object> params) {
        PageUtils pageUtils = attrService.getNoRelationAttr(params, attrgroupId);

        return R.ok().put("page", pageUtils);
    }


    /**
     * 删除某个组内的某个属性
     * http://localhost:88/api/product/attrgroup/attr/relation/delete
     *
     * @param relationVoList
     * @return
     */
    @PostMapping(value = "/attr/relation/delete")
    public R deleteRelation(@RequestBody List<AttrGroupRelationVo> relationVoList) {
        attrService.deleteRelation(relationVoList);

        return R.ok();
    }


    /**
     * 列表
     */
    @RequestMapping("/list/{catalogId}")
    public R list(@RequestParam Map<String, Object> params, @PathVariable("catalogId") Long catalogId) {
        PageUtils page = attrGroupService.queryPage(params, catalogId);


        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId) {
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        Long catelogId = attrGroup.getCatelogId();

        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        attrGroup.setCatelogPath(catelogPath);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
