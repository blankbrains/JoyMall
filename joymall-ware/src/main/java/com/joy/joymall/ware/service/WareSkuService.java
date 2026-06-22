package com.joy.joymall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.joy.common.to.OrderTo;
import com.joy.common.to.mq.StockDetailTo;
import com.joy.common.to.mq.StockLockedTo;
import com.joy.common.utils.PageUtils;
import com.joy.joymall.ware.entity.WareSkuEntity;
import com.joy.joymall.ware.vo.OrderVo;
import com.joy.joymall.ware.vo.SkuHasStockVo;
import com.joy.joymall.ware.vo.WareSkuLockRespVo;
import com.joy.joymall.ware.vo.WareSkuLockVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-04 18:29:13
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageByCondition(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    boolean orderLockStock(WareSkuLockVo wareSkuLockVo);

    void releaseLockedStock(StockLockedTo lockedTo) throws IOException;

    void releaseLockedStock(OrderTo orderVo);
}

