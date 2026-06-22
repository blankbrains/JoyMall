package com.joy.joymall.product.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.joy.joymall.product.service.CategoryBrandRelationService;
import com.joy.joymall.product.vo.Category2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.product.dao.CategoryDao;
import com.joy.joymall.product.entity.CategoryEntity;
import com.joy.joymall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.先查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2.组装成父子树形结构
        //2.1找到所有的一级分类,特点：父分类为0
        List<CategoryEntity> levelOneMenus = entities.stream()
                .filter((categoryEntity) -> categoryEntity.getParentCid() == 0)
                .peek((menu) -> {
                    //为每一个父分类设置子分类
                    menu.setChildren(getChildrens(menu, entities));
                })
                .sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort())))
                .collect(Collectors.toList());

        return levelOneMenus;
    }

    //获取当前父菜单的子分类,递归查找
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> childrenMenu = all.stream().filter((categoryEntity -> {
                    //过滤出当前菜单的子菜单
                    return categoryEntity.getParentCid().equals(root.getCatId());
                }))
                //每一个子菜单还有可以有他的子菜单，递归寻找
                .peek((menu) -> menu.setChildren(getChildrens(menu, all)))
                .sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort())))
                .collect(Collectors.toList());
        return childrenMenu;
    }



    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1.删除之前先检查当前菜单是否被其他菜单引用

        // 逻辑删除：@TableLogic 会将 deleteBatchIds 转换为 update showStatus=0
        baseMapper.deleteBatchIds(asList);
    }

    /**
     * 通过当前分类Id查出完整分类路径，一般为三级分类id，查出他的全部父id路径
     *
     * @param catelogId
     * @return
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        findParentPath(catelogId, paths);

        return paths.toArray(new Long[0]);
    }


    //更新删除某些缓存，法1，Caching可以设置多个缓存效果同时生效
//    @Caching(evict = {
//            @CacheEvict(cacheNames = {"category"}, key = "'getOneLevels'"),
//            @CacheEvict(cacheNames = {"category"}, key = "'getCatalogJson'")
//    })
    //法2,默认删除当前分区的所有缓存，有限制
    @CacheEvict(value = "category", allEntries = true)
    @Transactional
    @Override
    public void updateDetails(CategoryEntity category) {
        //先更新自身表
        this.updateById(category);

        //级联更新
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

        //TODO 以及其他级联表
    }

    //如果缓存没有，则放入，缓存有，不会调用，使用的是Spring的AOP
    //缓存的结果是jdk序列化后的内容，说明只适用于java
    //过期时间为永不过期
    @Cacheable(value = {"category"}, key = "#root.methodName", sync = true)
    @Override
    public List<CategoryEntity> getOneLevels() {
        System.out.println("获取所有一级分类。。。");

        return this.baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    /**
     * 带缓存
     *
     * @return ;
     */
    public Map<String, List<Category2Vo>> getCatalogJsonPrimordial() {
        /**
         * 三个问题
         * 穿透：不存在key，布隆过滤器或null值
         * 击穿，key过期，加锁
         * 雪崩，大量key过期，设置随机过期时间
         */
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            //缓存没有,从数据库查
            return getCatalogJsonFromDbWithRedissonLock();
        }
        //再转回对象
        TypeReference<Map<String, List<Category2Vo>>> typeReference = new TypeReference<Map<String, List<Category2Vo>>>() {
        };

        return JSON.parseObject(catalogJson, typeReference);
    }

    /**
     * 使用Redisson实现分布式锁
     * 缓存一致性问题：保证缓存和数据库数据一致
     * 1.双写模式 ：改完数据库改缓存
     * 2.失效模式 ： 改完数据库，删掉缓存
     *
     * @return ;
     */
    private Map<String, List<Category2Vo>> getCatalogJsonFromDbWithRedissonLock() {
        //1.查出所有分类
        //2.所有数据(包括1，2，3级分类)
        //对不同服务，锁名字一样，锁就一样
        RLock catalogLock = redissonClient.getLock("catalogJsonLock");
        catalogLock.lock();
        Map<String, List<Category2Vo>> catalogFromDb;
        try {
            catalogFromDb = getCatalogFromDb();
        } finally {
            catalogLock.unlock();
        }

        return catalogFromDb;
    }


    /**
     * 分布式锁
     *
     * @return ;
     */
    private Map<String, List<Category2Vo>> getCatalogJsonFromDbWithDistributedLock() {
        // 使用循环代替递归，避免 StackOverflow 风险
        int maxRetries = 10;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            String token = UUID.randomUUID().toString();
            Boolean setIfAbsent = redisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(setIfAbsent)) {
                try {
                    return getCatalogFromDb();
                } finally {
                    //使用lua脚本删锁保证原子性
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                            Collections.singletonList("lock"), token);
                }
            }
            retryCount++;
            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        // 超过重试次数时降级：直接从 DB 查询
        return getCatalogFromDb();
    }


    @Cacheable(cacheNames = {"category"}, key = "#root.methodName", sync = true)
    @Override
    public Map<String, List<Category2Vo>> getCatalogJson() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        //1.查出所有一级分类
        List<CategoryEntity> oneLevels = getParentCid(0L, categoryEntities);

        Map<String, List<Category2Vo>> categoryResultMap = oneLevels.stream()
                .collect(Collectors.toMap(k -> k.getCatId().toString(), levelOne -> {
                    //查询所有二级分类
                    List<CategoryEntity> categoryTwoLevelEntities = getParentCid(levelOne.getCatId(),
                            categoryEntities);

                    List<Category2Vo> category2Vos = null;
                    if (categoryTwoLevelEntities != null) {

                        category2Vos = categoryTwoLevelEntities.stream().map((levelTwo) -> {
                            //找出三级分类
                            List<CategoryEntity> categoryThreeLevelEntities = getParentCid(levelTwo.getCatId(),
                                    categoryEntities);

                            List<Category2Vo.Category3Vo> category3Vos = null;
                            if (categoryThreeLevelEntities != null) {
                                category3Vos = categoryThreeLevelEntities.stream().map((levelThree) -> {
                                    Category2Vo.Category3Vo category3Vo = new Category2Vo.Category3Vo(
                                            levelTwo.getCatId().toString(), levelThree.getCatId().toString(),
                                            levelThree.getName());
                                    return category3Vo;
                                }).collect(Collectors.toList());
                            }

                            //封装二级分类
                            Category2Vo category2Vo = new Category2Vo(
                                    levelOne.getCatId().toString(), category3Vos, levelTwo.getCatId().toString()
                                    , levelTwo.getName());

                            return category2Vo;
                        }).collect(Collectors.toList());
                    }
                    return category2Vos;
                }));
        return categoryResultMap;
    }

    /**
     * 查数据库
     *
     * @return ；
     */
    private Map<String, List<Category2Vo>> getCatalogFromDb() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (!StringUtils.isEmpty(catalogJson)) {
            TypeReference<Map<String, List<Category2Vo>>> typeReference = new TypeReference<Map<String, List<Category2Vo>>>() {
            };
            return JSON.parseObject(catalogJson, typeReference);
        }
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        //1.查出所有一级分类
        List<CategoryEntity> oneLevels = getParentCid(0L, categoryEntities);

        Map<String, List<Category2Vo>> categoryResultMap = oneLevels.stream()
                .collect(Collectors.toMap(k -> k.getCatId().toString(), levelOne -> {
                    //查询所有二级分类
                    List<CategoryEntity> categoryTwoLevelEntities = getParentCid(levelOne.getCatId(),
                            categoryEntities);

                    List<Category2Vo> category2Vos = null;
                    if (categoryTwoLevelEntities != null) {
                        //封装二级分类
                        category2Vos = categoryTwoLevelEntities.stream().map((levelTwo) -> {
                            //找出三级分类
                            List<CategoryEntity> categoryThreeLevelEntities = getParentCid(levelTwo.getCatId(),
                                    categoryEntities);

                            List<Category2Vo.Category3Vo> category3Vos = null;
                            if (categoryThreeLevelEntities != null) {
                                category3Vos = categoryThreeLevelEntities.stream().map((levelThree) -> {
                                    Category2Vo.Category3Vo category3Vo = new Category2Vo.Category3Vo(
                                            levelTwo.getCatId().toString(), levelThree.getCatId().toString(),
                                            levelThree.getName());
                                    return category3Vo;
                                }).collect(Collectors.toList());
                            }

                            //封装二级分类
                            Category2Vo category2Vo = new Category2Vo(
                                    levelOne.getCatId().toString(), category3Vos, levelTwo.getCatId().toString()
                                    , levelTwo.getName());

                            return category2Vo;
                        }).collect(Collectors.toList());
                    }
                    return category2Vos;
                }));
        //查完之后直接放入缓存，避免多次查库
        String toJSONString = JSON.toJSONString(categoryResultMap);
        redisTemplate.opsForValue().set("catalogJson", toJSONString, 1, TimeUnit.DAYS);
        return categoryResultMap;
    }


    /**
     * 单机锁
     *
     * @return
     */
    private Map<String, List<Category2Vo>> getCatalogJsonFromDbWithLoaclLock() {
        //查出所有分类
        //优化
        //所有数据(包括1，2，3级分类)
        //使用双检锁来保证线程安全
        //spring默认是单例，this对象也可以锁
        //锁住之后再进行检查，否则还是要查数据库
        //TODO 本地锁也无法解决数据一致性问题和多次查询问题
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (!StringUtils.isEmpty(catalogJson)) {
            TypeReference<Map<String, List<Category2Vo>>> typeReference = new TypeReference<Map<String, List<Category2Vo>>>() {
            };

            return JSON.parseObject(catalogJson, typeReference);
        }
        synchronized (this) {  //锁住之后还需要再判断，因为很有可能多个线程到此
            //再查询一次，得到最新的内容
            catalogJson = redisTemplate.opsForValue().get("catalogJson");
            if (!StringUtils.isEmpty(catalogJson)) {
                TypeReference<Map<String, List<Category2Vo>>> typeReference = new TypeReference<Map<String, List<Category2Vo>>>() {
                };

                return JSON.parseObject(catalogJson, typeReference);
            }
            System.out.println("查询数据库。。。");
            List<CategoryEntity> categoryEntities = baseMapper.selectList(null);


            //1.查出所有一级分类
            List<CategoryEntity> oneLevels = getParentCid(0L, categoryEntities);

            Map<String, List<Category2Vo>> categoryResultMap = oneLevels.stream()
                    .collect(Collectors.toMap(k -> k.getCatId().toString(), levelOne -> {
                        //查询所有二级分类
                        List<CategoryEntity> categoryTwoLevelEntities = getParentCid(levelOne.getCatId(),
                                categoryEntities);

                        List<Category2Vo> category2Vos = null;
                        if (categoryTwoLevelEntities != null) {
                            //封装二级分类
                            category2Vos = categoryTwoLevelEntities.stream().map((levelTwo) -> {
                                //找出三级分类
                                List<CategoryEntity> categoryThreeLevelEntities = getParentCid(levelTwo.getCatId(),
                                        categoryEntities);

                                List<Category2Vo.Category3Vo> category3Vos = null;
                                if (categoryThreeLevelEntities != null) {
                                    category3Vos = categoryThreeLevelEntities.stream().map((levelThree) -> {
                                        Category2Vo.Category3Vo category3Vo = new Category2Vo.Category3Vo(
                                                levelTwo.getCatId().toString(), levelThree.getCatId().toString(),
                                                levelThree.getName());
                                        return category3Vo;
                                    }).collect(Collectors.toList());
                                }

                                //封装二级分类
                                Category2Vo category2Vo = new Category2Vo(
                                        levelOne.getCatId().toString(), category3Vos, levelTwo.getCatId().toString()
                                        , levelTwo.getName());

                                return category2Vo;
                            }).collect(Collectors.toList());
                        }
                        return category2Vos;
                    }));
            //查完之后直接放入缓存，避免多次查库
            String toJSONString = JSON.toJSONString(categoryResultMap);
            redisTemplate.opsForValue().set("catalogJson", toJSONString, 1, TimeUnit.DAYS);
            return categoryResultMap;
        }
    }


    /**
     * 从已知集合找出父亲节点
     *
     * @param parentCid:传入的父节点
     * @param categoryEntities:所有分类
     * @return :当前父节点下的所有子节点
     */
    private List<CategoryEntity> getParentCid(Long parentCid, List<CategoryEntity> categoryEntities) {
        List<CategoryEntity> entities = categoryEntities.stream().filter((condition) -> {
            return condition.getParentCid().equals(parentCid);
        }).collect(Collectors.toList());

        return entities;
    }

    private void findParentPath(Long childrenId, List<Long> paths) {
        //通过当前id得到整个实体
        CategoryEntity entity = this.getById(childrenId);

        //查询有没有父分类
        if (entity.getParentCid() != 0) {
            findParentPath(entity.getParentCid(), paths);
        }
        paths.add(childrenId);
    }


}