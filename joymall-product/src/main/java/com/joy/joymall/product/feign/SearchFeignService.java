package com.joy.joymall.product.feign;

import com.joy.common.to.es.SkuEsModel;
import com.joy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/25 0:19
 */
@FeignClient("joymall-search")
public interface SearchFeignService {
    @PostMapping("/search/save/product")
    R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels);
}
