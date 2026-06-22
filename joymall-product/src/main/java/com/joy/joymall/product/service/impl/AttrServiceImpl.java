package com.joy.joymall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.joy.common.constant.ProductConstant;
import com.joy.joymall.product.dao.AttrAttrgroupRelationDao;
import com.joy.joymall.product.dao.AttrGroupDao;
import com.joy.joymall.product.dao.CategoryDao;
import com.joy.joymall.product.entity.AttrAttrgroupRelationEntity;
import com.joy.joymall.product.entity.AttrGroupEntity;
import com.joy.joymall.product.entity.CategoryEntity;
import com.joy.joymall.product.service.CategoryService;
import com.joy.joymall.product.vo.AttrGroupRelationVo;
import com.joy.joymall.product.vo.AttrResponseVo;
import com.joy.joymall.product.vo.AttrVo;
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

import com.joy.joymall.product.dao.AttrDao;
import com.joy.joymall.product.entity.AttrEntity;
import com.joy.joymall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Resource
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Resource
    private AttrGroupDao attrGroupDao;

    @Resource
    private CategoryDao categoryDao;

    @Autowired
    private CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveAttr(AttrVo attrVo) {
        //先保存基本信息，再保存分组信息
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attrVo, attrEntity);
        //保存基本数据
        this.save(attrEntity);
        //保存关联关系
        if (attrVo.getAttrType() == ProductConstant.AttrEnum.BASE_ATTR_TYPE.getCode() && attrVo.getAttrGroupId() != null) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attrVo.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationDao.insert(relationEntity);
        }
    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType) {
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>()
                .eq("attr_type", "base".equalsIgnoreCase(attrType) ? ProductConstant.AttrEnum.BASE_ATTR_TYPE.getCode() : ProductConstant.AttrEnum.SALE_ATTR_TYPE.getCode());

        if (catelogId != 0) {
            queryWrapper.eq("catelog_id", catelogId);
        }
        String key = (String) params.get("key");

        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            });
        }
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );
        //封装后的数据缺少分类名和分组名
        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        //最终封装的数据
        List<AttrResponseVo> responseVoList = records.stream().map((entity) -> {
            AttrResponseVo attrResponseVo = new AttrResponseVo();
            BeanUtils.copyProperties(entity, attrResponseVo);

            //设置分类和分组名称
            //获得关联中间表得到组id获得组名，然后再通过分类id得到分类名
            //销售属性无分组
            if ("base".equalsIgnoreCase(attrType)) {
                AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(
                        new QueryWrapper<AttrAttrgroupRelationEntity>()
                                .eq("attr_id", entity.getAttrId()));
                if (relationEntity != null && relationEntity.getAttrGroupId() != null) {
                    attrResponseVo.setGroupName(attrGroupDao.selectById(relationEntity.getAttrGroupId()).getAttrGroupName());
                }
            }

            CategoryEntity categoryEntity = categoryDao.selectById(entity.getCatelogId());
            if (categoryEntity != null) {

                attrResponseVo.setCatelogName(categoryEntity.getName());
            }
            return attrResponseVo;
        }).collect(Collectors.toList());
        //填充到结果集
        pageUtils.setList(responseVoList);
        return pageUtils;
    }

    @Override
    public AttrResponseVo getAttrInfo(Long attrId) {

        AttrResponseVo responseVo = new AttrResponseVo();
        AttrEntity attrEntity = this.getById(attrId);
        //先填充基本属性
        BeanUtils.copyProperties(attrEntity, responseVo);
        //接下来填充分类和分组
//        responseVo.setAttrGroupId();
//        responseVo.setCatelogPath();
        AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(
                new QueryWrapper<AttrAttrgroupRelationEntity>()
                        .eq("attr_id", attrEntity.getAttrId()));

        //设置分组信息
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.BASE_ATTR_TYPE.getCode()) {

            if (relationEntity != null) {
                responseVo.setAttrGroupId(relationEntity.getAttrGroupId());
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                if (attrGroupEntity != null) {

                    responseVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }

        //设置分类信息
        Long catelogId = attrEntity.getCatelogId();

        //递归得到整个id路径
        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        responseVo.setCatelogPath(catelogPath);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if (categoryEntity != null) {
            responseVo.setCatelogName(categoryEntity.getName());
        }
        return responseVo;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateAttr(AttrVo attr) {

        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);

        this.updateById(attrEntity);


        //1、修改分组关联,基本属性才有，销售没有
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.BASE_ATTR_TYPE.getCode()) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attr.getAttrId());


            Integer count = attrAttrgroupRelationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>()
                    .eq("attr_id", attr.getAttrId()));

            if (count > 0) {
                attrAttrgroupRelationDao.update(relationEntity,
                        new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            } else {
                attrAttrgroupRelationDao.insert(relationEntity);
            }
        }
    }


    /**
     * 根据分组id查找对应组内的所有属性
     *
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {

        //通过分组id查询到当前的分组（含属性）
        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>()
                        .eq("attr_group_id", attrgroupId));

        //通过分组筛选出所有的分组id，再查询到所有的属性
        List<Long> attrIds = relationEntities.stream()
                .map(AttrAttrgroupRelationEntity::getAttrId)
                .collect(Collectors.toList());

        if (attrIds.size() == 0) {
            return null;
        }
        return this.listByIds(attrIds);

    }

    @Override
    public void deleteRelation(List<AttrGroupRelationVo> relationVoList) {
        //需要多次查数据库，性能损耗
//        attrAttrgroupRelationDao.delete(
//                new QueryWrapper<AttrAttrgroupRelationEntity>());

        //抽取出所有组id以及属性id封装到关联关系表，进行批量删除
        List<AttrAttrgroupRelationEntity> relationEntities = relationVoList.stream().map((attrGroupRelationVo -> {
            AttrAttrgroupRelationEntity entity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(attrGroupRelationVo, entity);
            return entity;
        })).collect(Collectors.toList());
        attrAttrgroupRelationDao.deleteBatchRelation(relationEntities);
    }

    /**
     * 需要关联的属性，存在且其他组不能有关联
     * 排除同一个分类下的非当前组的所有属性，就是可以关联的属性
     *
     * @param params
     * @param attrgroupId
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        //1当前分组只能关联自己所属分类里面的所有属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        //2查询当前分组所属的分类，比如手机类，手机膜之类等等
        Long catelogId = attrGroupEntity.getCatelogId();
        //2.1、当前分类下的其他分组(排除当前分组)(可能当前分类下没有其他分组了，非空判断)
        List<AttrGroupEntity> groupEntities = attrGroupDao.selectList(
                new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        //2.2、这些分组关联的属性（也可能为空）
        List<Long> groupIds = groupEntities.stream()
                .map(AttrGroupEntity::getAttrGroupId)
                .collect(Collectors.toList());
        //通过组id得到其他关联组
        List<AttrAttrgroupRelationEntity> otherGroupEntities = attrAttrgroupRelationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", groupIds));
        //通过组获得所有关联的属性
        List<Long> attrIds = otherGroupEntities.stream().
                map((AttrAttrgroupRelationEntity::getAttrId))
                .collect(Collectors.toList());
        //2.3、移除这些以及关联的属性,得到的就是可关联的属性
        //只能关联别的分组没有引用的属性
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>()
                .eq("catelog_id", catelogId)
                .eq("attr_type", ProductConstant.AttrEnum.BASE_ATTR_TYPE.getCode());

        if (attrIds.size() > 0) {
            queryWrapper.notIn("attr_id", attrIds);
        }

        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper -> {
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            }));
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), queryWrapper);
        return new PageUtils(page);
    }

    @Override
    public List<Long> selectSearchByAttrIds(List<Long> attIds) {

        return baseMapper.selectSearchByAttrIds(attIds);
    }
}