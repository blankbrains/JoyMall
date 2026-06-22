package com.joy.joymall.product;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.joy.joymall.product.dao.AttrGroupDao;
import com.joy.joymall.product.dao.SkuSaleAttrValueDao;
import com.joy.joymall.product.entity.BrandEntity;
import com.joy.joymall.product.service.BrandService;
import com.joy.joymall.product.service.CategoryService;
import com.joy.joymall.product.vo.SkuItemVo;
import org.junit.jupiter.api.Test;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class JoymallProductApplicationTests {

    @Autowired
    private BrandService brandService;


    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Resource
    private AttrGroupDao attrGroupDao;

    @Resource
    private SkuSaleAttrValueDao skuSaleAttrValueDao;


    @Test
    void testRedis() {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        valueOperations.set("hello", "world" + UUID.randomUUID().toString());
        String hello = valueOperations.get("hello");
        System.out.println(hello);
    }

    @Test
    void contextLoads() {
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        System.out.println(Arrays.toString(catelogPath));
    }


    @Test
    void findPath() {

    }


    @Test
    void testRedisson() {
        System.out.println(redissonClient);
        RSemaphore park = redissonClient.getSemaphore("park");
        try {
            park.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    void testDao() {
//        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setDescript("测试品牌数据");
//        brandEntity.setLogo("djl");
//        brandEntity.setName("测试品牌");
//        brandService.save(brandEntity);
        List<BrandEntity> li = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1));
        li.forEach(System.out::println);
    }


//    @Test
//    void testFile() throws FileNotFoundException {
//
////        // Endpoint以杭州为例，其它Region请按实际情况填写。
////        String endpoint = "http://oss-cn-qingdao.aliyuncs.com";
////        // 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
////        String accessKeyId = "your-access-key";
////        String accessKeySecret = "your-secret-key";
//
//        // 创建OSSClient实例。
////        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
//
//        // 上传文件流。
//        InputStream inputStream = new FileInputStream("C:\\path\\to\\test\\image.jpg");
//        ossClient.putObject("joymall", "chicken.jpg", inputStream);
//
//        // 关闭OSSClient。
//        ossClient.shutdown();
//
//        System.out.println("上传成功~~");
//    }


    @Test
    void testAttr() {
        List<SkuItemVo.SpuItemAttrGroupVo> attrs = attrGroupDao.getAttrGroupWithAttrsBySpuId(13L, 225L);
        System.out.println(attrs);
    }

    @Test
    void testSpuAttr() {
        List<SkuItemVo.SkuItemSaleAttrVo> skuAttrs = skuSaleAttrValueDao.getSaleAttrsBySpuId(13L);
        System.out.println(skuAttrs);
    }

}
