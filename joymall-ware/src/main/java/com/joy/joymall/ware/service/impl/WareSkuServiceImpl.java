package com.joy.joymall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.joy.common.exception.NoStockException;
import com.joy.common.to.OrderTo;
import com.joy.common.to.mq.StockDetailTo;
import com.joy.common.to.mq.StockLockedTo;
import com.joy.common.utils.R;

import com.joy.joymall.ware.config.MyRabbitConfig;
import com.joy.joymall.ware.entity.WareOrderTaskDetailEntity;
import com.joy.joymall.ware.entity.WareOrderTaskEntity;
import com.joy.joymall.ware.feign.OrderFeignService;
import com.joy.joymall.ware.feign.SkuInfoFeign;
import com.joy.joymall.ware.service.WareOrderTaskDetailService;
import com.joy.joymall.ware.service.WareOrderTaskService;
import com.joy.joymall.ware.vo.OrderItemVo;
import com.joy.joymall.ware.vo.OrderVo;
import com.joy.joymall.ware.vo.SkuHasStockVo;

import com.joy.joymall.ware.vo.WareSkuLockVo;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.ware.dao.WareSkuDao;
import com.joy.joymall.ware.entity.WareSkuEntity;
import com.joy.joymall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {


    @Autowired
    private SkuInfoFeign skuInfoFeign;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    private WareOrderTaskService wareOrderTaskService;

    @Autowired
    private OrderFeignService orderFeignService;

    public static final Logger log = LoggerFactory.getLogger(WareSkuServiceImpl.class);


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();

        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId) && Long.parseLong(skuId) > 0) {
            queryWrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");

        if (!StringUtils.isEmpty(wareId) && Long.parseLong(wareId) > 0) {
            queryWrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * @param skuId  那个商品id
     * @param wareId 那个仓库id
     * @param skuNum 存放多少件
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        List<WareSkuEntity> wareSkuEntities = this.baseMapper.selectList(
                new QueryWrapper<WareSkuEntity>()
                        .eq("sku_id", skuId)
                        .eq("ware_id", wareId));
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            //TODO 远程调用补全采购项名称
            try {
                R info = skuInfoFeign.info(skuId);
                Map<String, Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {

                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            } catch (Exception e) {

            }
            this.baseMapper.insert(wareSkuEntity);
        } else {

            this.baseMapper.addStock(skuId, wareId, skuNum);
        }
    }

    //通过skuid查出库存量判断还有没有库存
    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> skuHasStockVos = skuIds.stream().map((skuId) -> {
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();
            Long count = this.baseMapper.getSukStock(skuId);
            skuHasStockVo.setSkuId(skuId);
            skuHasStockVo.setHasStock(count != null && count > 0);
            return skuHasStockVo;
        }).collect(Collectors.toList());

        return skuHasStockVos;
    }


    /**
     * 为某个订单锁定库存
     *
     * @param wareSkuLockVo
     * @return 库存解锁的场景：
     * 1.下订单成功，订单过期没有支付被系统取消/被用户手动取消
     * 2.下订单成功，库存也锁定成功，但由于其它业务调用（如扣积分之类）失败，导致订单回滚，订单锁定就要解锁
     */
    @Override
    @Transactional(rollbackFor = NoStockException.class)
    public boolean orderLockStock(WareSkuLockVo wareSkuLockVo) {
        //保存库存工作单，方便解锁回复，留一个记录
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(wareSkuLockVo.getOrderSn());
        wareOrderTaskService.save(wareOrderTaskEntity);
        //1.找到每个商品在那个仓库有库存
        List<OrderItemVo> lockVos = wareSkuLockVo.getLockVos();
        List<SkuWareHasStock> skuWareHasStocks = findAllWareId(lockVos);
        //2.锁定库存
        return lockWareStock(skuWareHasStocks, wareOrderTaskEntity);
    }

    /**
     * 下订单成功，库存也锁定成功，但由于其它业务调用（如扣积分之类）失败，导致订单回滚，订单锁定就要解锁
     * <p>
     * 只要收到消息，就会自动ack，可能会造成消息丢失，但是解锁失败
     *
     * @param lockedTo : 锁定的库存信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void releaseLockedStock(StockLockedTo lockedTo) {
        log.info("收到解锁库存的消息");
        //库存工作单id
        StockDetailTo wareTaskDetail = lockedTo.getDetailTo();
        //先查询数据库，如果数据库有，说明是其它业务异常或者用户取消，如果没有，说明是因为锁定库存失败回滚导致的，这种情况无需解锁，只需消费消息
        Long wareTaskDetailId = wareTaskDetail.getId();
        WareOrderTaskDetailEntity stockTaskDetail = wareOrderTaskDetailService.getById(wareTaskDetailId);
        if (!ObjectUtils.isEmpty(stockTaskDetail)) {
            //解锁情况
            //订单状态表存在记录
            //      没有这个订单，必须解锁（说明锁库存没回滚，但是订单回滚了，可能其它业务出问题）
            //      有这个订单，判断订单状态
            //              已取消：解锁
            //              其余：不解锁
            //订单状态表没记录，说明库存锁定失败回滚了，无需解锁
            Long wareTaskId = lockedTo.getId();
            WareOrderTaskEntity wareOrderTaskEntity = wareOrderTaskService.getById(wareTaskId);
            String orderSn = wareOrderTaskEntity.getOrderSn();
            //根据订单号查询订单状态
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                //订单数据返回成功
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });

                if (ObjectUtils.isEmpty(data) || data.getStatus() == 4) {
                    //订单不存在或者订单被取消，才能解锁库存
                    if (stockTaskDetail.getLockStatus() == 1) {
                        //只有已锁定状态才能解锁
                        releaseLockedStock(wareTaskDetail);
                    }
                }
            } else {
                throw new RuntimeException("远程服务调用失败");
            }
        } else {
            //无需解锁
        }
    }

    /**
     * 防止订单服务卡顿，导致订单消息一直改不了，库存解锁优先到期，解锁不了库存
     *
     * @param orderVo ：
     */
    @Override
    public void releaseLockedStock(OrderTo orderVo) {
        String orderSn = orderVo.getOrderSn();
        //查询一下库存工作单详情的状态，如果已经解锁了，就不用重复解锁了
        WareOrderTaskEntity orderTaskEntity = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);
        Long orderTaskEntityId = orderTaskEntity.getId();
        //根据工作单id查询所有没有解锁的工作单详情
        List<WareOrderTaskDetailEntity> orderTaskDetailEntities = wareOrderTaskDetailService.getByOrderTaskIdConditionOfUnLocked(orderTaskEntityId);
        if (!CollectionUtils.isEmpty(orderTaskDetailEntities)) {
            for (WareOrderTaskDetailEntity orderTaskDetailEntity : orderTaskDetailEntities) {
                StockDetailTo stockDetailTo = new StockDetailTo();
                //设置已解锁
//                stockDetailTo.setLockStatus(2);
//                stockDetailTo.setSkuId(orderTaskDetailEntity.getSkuId());
//                stockDetailTo.setSkuNum(orderTaskDetailEntity.getSkuNum());
//                stockDetailTo.setWareId(orderTaskDetailEntity.getWareId());
//                stockDetailTo.setTaskId(orderTaskDetailEntity.getTaskId());
//                stockDetailTo.setId(orderTaskDetailEntity.getId());
                BeanUtils.copyProperties(orderTaskDetailEntity,stockDetailTo);
                releaseLockedStock(stockDetailTo);
            }
        }
    }

    /**
     * 解锁库存
     *
     * @param wareTaskDetail ： 订单锁定详情实体
     */
    private void releaseLockedStock(StockDetailTo wareTaskDetail) {
        //解锁库存
        this.baseMapper.releaseLockedStock(wareTaskDetail);
        //更新库存工作单详情为已解锁
        WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity();
        detailEntity.setId(wareTaskDetail.getId());
        detailEntity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(detailEntity);
    }

    /**
     * 查询所有锁定sku的仓库
     *
     * @param lockVos
     * @return
     */
    private List<SkuWareHasStock> findAllWareId(List<OrderItemVo> lockVos) {
        List<SkuWareHasStock> skuWareHasStocks = lockVos.stream().map((wareVo) -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = wareVo.getSkuId();
            stock.setSkuId(skuId);
            stock.setCount(wareVo.getCount());
            //查询这个商品在哪里有库存
            List<Long> wareSkuHasStockIds = this.baseMapper.listWareIdsHasStock(skuId);
            stock.setWareIds(wareSkuHasStockIds);
            return stock;
        }).collect(Collectors.toList());
        return skuWareHasStocks;
    }

    /**
     * 锁定库存
     *
     * @param skuWareHasStocks ： 当前商品所存在的所有仓库
     * @return ： 锁定状态
     */
    private boolean lockWareStock(List<SkuWareHasStock> skuWareHasStocks, WareOrderTaskEntity wareOrderTaskEntity) {
        for (SkuWareHasStock hasStock : skuWareHasStocks) {
            boolean singleSkuLockStock = false;
            //当前skuId和所在库存
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareIds();
            Integer count = hasStock.getCount();
            //没有仓库有库存
            if (CollectionUtils.isEmpty(wareIds)) {
                throw new NoStockException(skuId);
            }
            //对每一个在的库存遍历，如果有余货就锁定
            //如果有锁定失败的，就会回滚锁定，发出去的消息，即使解锁也找不到id
            for (Long wareId : wareIds) {
                //锁定成功返回1，否则返回0
                Long effectLines = this.baseMapper.lockSkuStock(skuId, wareId, count);
                if (effectLines == 1) {
                    singleSkuLockStock = true;
                    //TODO 告诉MQ库存锁定成功
                    //留一个工作详情，以便解锁需要
                    WareOrderTaskDetailEntity lockRecord = new WareOrderTaskDetailEntity(null, skuId, null, count, wareOrderTaskEntity.getId(), wareId, 1);
                    wareOrderTaskDetailService.save(lockRecord);
                    //锁定信息发送MQ（在事务提交后发送，避免事务回滚导致消息无法撤销）
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    //订单保存id
                    stockLockedTo.setId(wareOrderTaskEntity.getId());
                    //订单详情,这里不只设置订单详情id的原因就是，如果是远程锁定某些东西，但是本地回滚了，且消息发送了，远程就没办法解锁了
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(lockRecord, stockDetailTo);
                    stockLockedTo.setDetailTo(stockDetailTo);
                    if (TransactionSynchronizationManager.isSynchronizationActive()) {
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                rabbitTemplate.convertAndSend(MyRabbitConfig.EXCHANGE, MyRabbitConfig.EXCHANGE_BINDING_DELAY_QUEUE_ROUTING_KEY, stockLockedTo);
                            }
                        });
                    } else {
                        rabbitTemplate.convertAndSend(MyRabbitConfig.EXCHANGE, MyRabbitConfig.EXCHANGE_BINDING_DELAY_QUEUE_ROUTING_KEY, stockLockedTo);
                    }
                    break;
                } else {
                    //当前仓库锁失败，可能库存不足,锁定下一个仓库
                }
            }
            //
            if (!singleSkuLockStock) {
                //当前商品的所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }
        //所有商品都锁定成功
        return true;
    }

    @Data
    static
    class SkuWareHasStock {
        private Long skuId;

        /**
         * 所有存在商品的仓库号
         */
        private List<Long> wareIds;

        private Integer count;
    }
}