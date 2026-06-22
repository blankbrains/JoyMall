package com.joy.joymall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.joy.common.constant.ProductConstant;
import com.joy.common.to.SkuHasStockTo;
import com.joy.common.to.SkuReductionTo;
import com.joy.common.to.SpuBoundsTo;
import com.joy.common.to.es.SkuEsModel;
import com.joy.common.utils.R;
import com.joy.joymall.product.entity.*;
import com.joy.joymall.product.feign.CouponFeignService;
import com.joy.joymall.product.feign.SearchFeignService;
import com.joy.joymall.product.feign.WareFeignService;
import com.joy.joymall.product.service.*;
import com.joy.joymall.product.vo.SpuSaveVo;
import com.joy.joymall.product.vo.spuentity.Attr;
import com.joy.joymall.product.vo.spuentity.BaseAttrs;
import com.joy.joymall.product.vo.spuentity.Bounds;
import com.joy.joymall.product.vo.spuentity.Skus;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private SpuImagesService spuImagesService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    @Autowired
    private SkuInfoService skuInfoService;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * TODO     高级篇完善
     *
     * @param spuSaveVo
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo spuSaveVo) {
        //1.保存spu基本信息 pms_sku_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(spuSaveVo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(spuInfoEntity);

        //2.保存spu描述图片 pms_spu_info_desc
        List<String> spuImageDesc = spuSaveVo.getDecript();
        if (spuImageDesc != null && !spuImageDesc.isEmpty()) {
            SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
            spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
            spuInfoDescEntity.setDecript(String.join(",", spuImageDesc));
            spuInfoDescService.saveSpuInfoDesc(spuInfoDescEntity);
        }

        //3.保存spu图片集  pms_spu_images
        List<String> images = spuSaveVo.getImages();
        if (images != null && !images.isEmpty()) {
            spuImagesService.saveImages(spuInfoEntity.getId(), images);
        }


        //4.保存spu的规格参数  pms_product_attr_value
        List<BaseAttrs> baseAttrs = spuSaveVo.getBaseAttrs();
        if (baseAttrs != null && !baseAttrs.isEmpty()) {
            List<ProductAttrValueEntity> productAttrValueEntities = baseAttrs.stream().map((entity) -> {
                ProductAttrValueEntity attrValueEntity = new ProductAttrValueEntity();
                attrValueEntity.setAttrId(entity.getAttrId());
                //通过属性id得到属性名
                AttrEntity entityById = attrService.getById(entity.getAttrId());
                attrValueEntity.setAttrName(entityById != null ? entityById.getAttrName() : "");
                attrValueEntity.setAttrValue(entity.getAttrValues());
                attrValueEntity.setQuickShow(entity.getShowDesc());
                attrValueEntity.setSpuId(spuInfoEntity.getId());
                return attrValueEntity;
            }).collect(Collectors.toList());
            productAttrValueService.saveProductAttr(productAttrValueEntities);
        }

        //5.保存当前spu的所有sku
        List<Skus> skus = spuSaveVo.getSkus();
        if (skus != null && skus.size() > 0) {
            //得到默认图片
            for (Skus item : skus) {
                AtomicReference<String> defaultImg = new AtomicReference<>("");
                item.getImages().forEach(img -> {
                    if (img.getDefaultImg() == 1) {
                        defaultImg.set(img.getImgUrl());
                    }
                });
                //封装基本信息
                //5.1 sku基本信息 pms_sku_info
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg.get());
                //保存基本信息并得到主键
                skuInfoService.saveSkuInfo(skuInfoEntity);


                //5.2 sku图片信息 pms_sku_images
                //得到主键，为当前sku设置默认图片
                Long skuId = skuInfoEntity.getSkuId();
                //设置默认图片并保存图片,需要先知道是那个sku，即需要知道id
                List<SkuImagesEntity> skuImagesEntities = item.getImages().stream().map((img) -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    //因为没有外键关联，用的都是中间表，所以这些关联的主键都需要自己保存
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(imgUrl -> {
                    return !StringUtils.isEmpty(imgUrl.getImgUrl());
                }).collect(Collectors.toList());

                skuImagesService.saveBatch(skuImagesEntities);

                //5.3 sku销售属性信息 pms_sku_sale_attr_value
                List<Attr> attrs = item.getAttr();

                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attrs.stream().map((attr) -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());

                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

                //5.4 sku优惠，满减等信息，需要跨库，即openfeign调用
                //保存spu的积分信息
                Bounds bounds = spuSaveVo.getBounds();
                SpuBoundsTo spuBoundsTo = new SpuBoundsTo();
                BeanUtils.copyProperties(bounds, spuBoundsTo);
                spuBoundsTo.setSpuId(spuInfoEntity.getId());

                couponFeignService.saveSpuBounds(spuBoundsTo);

                //保存sku的积分优惠信息
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                //没有满减不要保存到数据库
                if (skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal(0)) > 0) {
                    R saveResult = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (saveResult.getCode() != 0) {
                        log.error("积分优惠错误");
                    }
                }
            }
        }
    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> queryWrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            //用and是为了让多个条件用()包裹起来
            queryWrapper.and((wrapper) -> {
                wrapper.eq("id", key).or().like("spu_name", key);
            });
        }

        String status = (String) params.get("status");
        if (!StringUtils.isEmpty(status)) {
            queryWrapper.eq("publish_status", status);

        }

        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId)) {
            if (Long.parseLong(brandId) > 0) {

                queryWrapper.eq("brand_id", brandId);
            }
        }

        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId)) {
            if (Long.parseLong(catelogId) > 0) {

                queryWrapper.eq("catalog_id", catelogId);
            }
        }


        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);

    }

    /**
     * 商品上架
     *
     * @param spuId
     */
    @Override
    public void up(Long spuId) {


        //根据spuId得到所有sku
        List<SkuInfoEntity> skuInfoEntities = skuInfoService.getSkusBySpuId(spuId);
        //查询当前sku的所有可以被检索的规格属性
        //1.先查出来所有属性
        List<ProductAttrValueEntity> attrValueEntities = productAttrValueService.baseAttrList(spuId);
        //下面这个可以避免重写多余方法
////        List<AttrEntity> attrEntities = attrValueEntities.stream().map((entity) -> {
////            Long attrId = entity.getAttrId();
////            return attrService.getById(attrId);
////            //2.保留可以被检索的
////        }).filter(attrEntity -> {
////            return attrEntity.getSearchType() == 1;
////        }).collect(Collectors.toList());

        //查询所有属性id
        List<Long> attIds = attrValueEntities.stream()
                .map(ProductAttrValueEntity::getAttrId)
                .collect(Collectors.toList());

        //查询出所有可被检索的id
        List<Long> searchAttrIds = attrService.selectSearchByAttrIds(attIds);

        Set<Long> ids = new HashSet<>(searchAttrIds);

        List<SkuEsModel.Attrs> attrsList = attrValueEntities.stream()
                .filter((item) -> {
                    return ids.contains(item.getAttrId());
                }).map((entity) -> {
                    SkuEsModel.Attrs attrs = new SkuEsModel.Attrs();
//                    attrs.setAttrId(entity.getAttrId());
//                    attrs.setAttrName(entity.getAttrName());
//                    attrs.setAttrValue(entity.getAttrValue());
                    BeanUtils.copyProperties(entity, attrs);
                    return attrs;
                }).collect(Collectors.toList());

        //查询出所有可以被检索的id是否有库存
        List<Long> skuIds = skuInfoEntities.stream().map(SkuInfoEntity::getSkuId)
                .collect(Collectors.toList());

        //避免远程调用出现错误
        Map<Long, Boolean> stockMap = null;
        try {
            R skuHasStock = wareFeignService.getSkuHasStock(skuIds);
            TypeReference<List<SkuHasStockTo>> typeReference = new TypeReference<List<SkuHasStockTo>>() {
            };
            stockMap = skuHasStock.getData(typeReference).stream().
                    collect(Collectors.toMap(SkuHasStockTo::getSkuId, SkuHasStockTo::isHasStock));
        } catch (Exception e) {
            log.error("库存服务查询出问题" + e.getMessage());
        }

        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skuInfoEntities.stream().map((entity) -> {
            //组装需要的数据
            SkuEsModel skuEsModel = new SkuEsModel();
            BeanUtils.copyProperties(entity, skuEsModel);
            skuEsModel.setSkuPrice(entity.getPrice());
            skuEsModel.setSkuImg(entity.getSkuDefaultImg());
            //TODO 1.发送远程调用库存是否还有  2.热度评分 0
            skuEsModel.setHotScore(0L);
            skuEsModel.setHasStock(finalStockMap != null && finalStockMap.get(entity.getSkuId()) != null
                    ? finalStockMap.get(entity.getSkuId()) : true);

            //保存品牌信息
            BrandEntity brandEntity = brandService.getById(entity.getBrandId());
            if (brandEntity != null) {
                skuEsModel.setBrandImg(brandEntity.getLogo());
                skuEsModel.setBrandName(brandEntity.getName());
            }

            //保存分类信息
            CategoryEntity categoryEntity = categoryService.getById(entity.getCatalogId());
            if (categoryEntity != null) {
                skuEsModel.setCatalogName(categoryEntity.getName());
            }

            //保存检索属性信息
            skuEsModel.setAttrs(attrsList);
            return skuEsModel;
        }).collect(Collectors.toList());

        //发送给es进行保存
        R r = searchFeignService.productStatusUp(upProducts);

        if (r.getCode() == 0) {
            //调用成功,TODO 改变商品发布状态
            this.baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.UP.getCode());
        } else {
            //TODO 重复调用
        }
    }


    /**
     * 根据skuId查询spu信息
     * @param skuId
     * @return
     */
    @Override
    public SpuInfoEntity getSpuBySkuId(Long skuId) {
        //1.查询SkuEntity得到SpuId
        SkuInfoEntity skuInfoEntity = skuInfoService.getById(skuId);
        if (skuInfoEntity == null) {
            return null;
        }
        //2.通过SpuId查询SpuEntity
        Long spuId = skuInfoEntity.getSpuId();
        SpuInfoEntity spuInfoEntity = this.baseMapper.selectById(spuId);
        return spuInfoEntity;
    }
}