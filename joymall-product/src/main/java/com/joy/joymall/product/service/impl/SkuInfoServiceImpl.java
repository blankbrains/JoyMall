package com.joy.joymall.product.service.impl;


import com.alibaba.fastjson.TypeReference;
import com.joy.common.utils.R;
import com.joy.joymall.product.config.mythreadpool.MyThreadPool;
import com.joy.joymall.product.entity.SkuImagesEntity;
import com.joy.joymall.product.entity.SpuInfoDescEntity;
import com.joy.joymall.product.feign.SecKillFeignService;
import com.joy.joymall.product.service.*;
import com.joy.joymall.product.vo.SecKillInfoVo;
import com.joy.joymall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.product.dao.SkuInfoDao;
import com.joy.joymall.product.entity.SkuInfoEntity;
import org.springframework.util.StringUtils;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private SecKillFeignService secKillFeignService;

    @Autowired
    private ThreadPoolExecutor executor; //对于线程池，自己注入就用自己的，否则用默认的


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> queryWrapper = new QueryWrapper<>();


        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                wrapper.eq("sku_id", key).or().like("spu_name", key);
            });
        }

        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId)) {
            if (Long.parseLong(catelogId) > 0) {
                queryWrapper.eq("catalog_id", catelogId);
            }
        }

        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId)) {
            if (Long.parseLong(brandId) > 0) {
                queryWrapper.eq("brand_id", brandId);
            }
        }

        String min = (String) params.get("min");
        if (!StringUtils.isEmpty(min)) {
            queryWrapper.ge("price", min);
        }

        String max = (String) params.get("max");
        if (!StringUtils.isEmpty(max)) {
            try {
                BigDecimal bigDecimal = new BigDecimal(max);
                if (bigDecimal.compareTo(new BigDecimal(0)) > 0) {
                    queryWrapper.le("price", max);
                }
            } catch (Exception e) {

            }
        }


        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {

        List<SkuInfoEntity> skuInfoEntities = this.list(
                new QueryWrapper<SkuInfoEntity>()
                        .eq("spu_id", spuId));

        return skuInfoEntities;
    }

    /**
     * 异步编排执行
     *
     * @param skuId
     * @return
     */
    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo();

        //需要等待它的执行结果的四个任务种它最先执行，其余三个异步执行
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            //1.查询Sku基本信息  pms_sku_info
            SkuInfoEntity info = getById(skuId);
            skuItemVo.setInfo(info);
            return info;
        }, executor);

        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((info) -> {
            //3.查询spu的销售属性组合
            List<SkuItemVo.SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService
                    .getSaleAttrsBySpuId(info.getSpuId());
            skuItemVo.setSaleAttr(saleAttrVos);
        }, executor);//不要再此之后then，否则和串行没区别，即等待它完成


        CompletableFuture<Void> spuDescFuture = infoFuture.thenAcceptAsync((info) -> {
            //4.获取spu介绍
            SpuInfoDescEntity desc = spuInfoDescService.getById(info.getSpuId());
            skuItemVo.setDesc(desc);
        }, executor);

        CompletableFuture<Void> spuAttrsFuture = infoFuture.thenAcceptAsync((info) -> {
            //5.获取spu规格参数信息
            List<SkuItemVo.SpuItemAttrGroupVo> groupAttrs = attrGroupService
                    .getAttrGroupWithAttrsBySpuId(info.getSpuId(), info.getCatalogId());
            skuItemVo.setGroupAttrs(groupAttrs);
        }, executor);


        //自己单独异步执行
        CompletableFuture<Void> skuImageFuture = CompletableFuture.runAsync(() -> {
            //2.sku的图片信息  pms_sku_images
            List<SkuImagesEntity> images = skuImagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(images);
        }, executor);


        CompletableFuture<Void> skuSecKillInfoFuture = CompletableFuture.runAsync(() -> {
            //查询sku秒杀信息
            R r = secKillFeignService.getSkuSecKillInfo(skuId);
            if (r.getCode() == 0) {
                SecKillInfoVo secKillInfoVo = r.getData(new TypeReference<SecKillInfoVo>() {
                });
                skuItemVo.setSecKillInfoVo(secKillInfoVo);
            }
        }, executor);
        //等待所有任务都完成，才能返回vo,get阻塞等结果

        CompletableFuture.allOf(saleAttrFuture, spuAttrsFuture, skuImageFuture, spuDescFuture, skuSecKillInfoFuture)
                .get();


        return skuItemVo;
    }

    @Override
    public BigDecimal getSkuPriceBySkuId(Long skuId) {
        SkuInfoEntity skuInfoEntity = this.getById(skuId);

        return skuInfoEntity.getPrice();
    }
}