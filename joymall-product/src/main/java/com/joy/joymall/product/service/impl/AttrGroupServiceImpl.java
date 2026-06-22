package com.joy.joymall.product.service.impl;

import com.joy.joymall.product.entity.AttrEntity;
import com.joy.joymall.product.service.AttrAttrgroupRelationService;
import com.joy.joymall.product.service.AttrService;
import com.joy.joymall.product.vo.AttrGroupWithAttrsVo;
import com.joy.joymall.product.vo.SkuItemVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.product.dao.AttrGroupDao;
import com.joy.joymall.product.entity.AttrGroupEntity;
import com.joy.joymall.product.service.AttrGroupService;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Resource
    private AttrGroupDao attrGroupDao;


    @Autowired
    private AttrAttrgroupRelationService attrAttrgroupRelationService;

    @Autowired
    private AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catalogId) {
        QueryWrapper<AttrGroupEntity> queryWrapper = new QueryWrapper<>();
        //模糊匹配
        String key = (String) params.get("key");
        //条件匹配
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((obj) -> {
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }
        if (catalogId == 0) {
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    queryWrapper
            );
            return new PageUtils(page);
        } else {
            queryWrapper.eq("catelog_id", catalogId);
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params), queryWrapper
            );
            return new PageUtils(page);
        }
    }

    //根据三级分类查询所有属性分组进而查询所有属性
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {
        //通过分类id查询所有属性分组
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(
                new QueryWrapper<AttrGroupEntity>()
                        .eq("catelog_id", catelogId));

//        封装vo
        List<AttrGroupWithAttrsVo> vos = attrGroupEntities.stream().map((entity) -> {
            AttrGroupWithAttrsVo attrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(entity, attrsVo);
            //查询分组内的所有属性
            List<AttrEntity> attrEntities = attrService.getRelationAttr(entity.getAttrGroupId());
            attrsVo.setAttrs(attrEntities);
            return attrsVo;
        }).collect(Collectors.toList());

        return vos;
    }

    @Override
    public List<SkuItemVo.SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        /**
         * 查出当前Spu对应的所有属性组和其属性
         */
        return this.baseMapper.getAttrGroupWithAttrsBySpuId(spuId, catalogId);

    }

}