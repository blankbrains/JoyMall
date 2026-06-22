package com.joy.joymall.product.feign.fallback;

import com.joy.common.exception.BizCodeEnum;
import com.joy.common.utils.R;
import com.joy.joymall.product.feign.SecKillFeignService;
import org.springframework.stereotype.Component;

/**
 * @Description: 每个远程方法调用失败后的兜底处理
 * @Author:joymall
 * @Date:2022/9/18 0:35
 */
@Component
public class SecKillFeignServiceFallBack implements SecKillFeignService {
    @Override
    public R getSkuSecKillInfo(Long skuId) {
        return R.error(BizCodeEnum.TOO_MANY_REQUEST.getCode(), BizCodeEnum.TOO_MANY_REQUEST.getMsg());
    }
}
