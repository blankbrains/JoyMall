package com.joy.joymall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.product.dao.ProductAttrValueDao;
import com.joy.joymall.product.entity.ProductAttrValueEntity;
import com.joy.joymall.product.service.ProductAttrValueService;
import org.springframework.transaction.annotation.Transactional;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveProductAttr(List<ProductAttrValueEntity> productAttrValueEntities) {
        this.saveBatch(productAttrValueEntities);
    }

    @Override
    public List<ProductAttrValueEntity> baseAttrList(Long spuId) {

        List<ProductAttrValueEntity> productAttrValueEntities = this.baseMapper.selectList(
                new QueryWrapper<ProductAttrValueEntity>()
                        .eq("spu_id", spuId));

        return productAttrValueEntities;
    }

    @Transactional
    @Override
    public void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities) {
        //1.删除之前spu对应的属性
        this.baseMapper.delete(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
        //2.保存当前更改后的spu属性

        List<ProductAttrValueEntity> productAttrValueEntities = entities.stream().peek((entity) -> {
            entity.setSpuId(spuId);
        }).collect(Collectors.toList());

        this.saveBatch(productAttrValueEntities);
    }
}