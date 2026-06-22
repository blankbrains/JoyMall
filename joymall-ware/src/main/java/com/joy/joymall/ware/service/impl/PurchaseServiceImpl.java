package com.joy.joymall.ware.service.impl;

import com.joy.common.constant.WareConstant;
import com.joy.joymall.ware.entity.PurchaseDetailEntity;
import com.joy.joymall.ware.service.PurchaseDetailService;
import com.joy.joymall.ware.service.WareSkuService;
import com.joy.joymall.ware.vo.MergeVo;
import com.joy.joymall.ware.vo.purchasevo.PurchaseCompleteVo;
import com.joy.joymall.ware.vo.purchasevo.PurchaseItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.ware.dao.PurchaseDao;
import com.joy.joymall.ware.entity.PurchaseEntity;
import com.joy.joymall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    private PurchaseDetailService purchaseDetailService;

    @Autowired
    private WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceived(Map<String, Object> params) {

        QueryWrapper<PurchaseEntity> queryWrapper = new QueryWrapper<>();
        //采购单刚新建或刚分配
        queryWrapper.eq("status", 0).or().eq("status", 1);

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();

        if (purchaseId == null) {
            //无采购单，新建
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(WareConstant.PurchaseEnum.CREATED.getCode());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();

        }
        //最后统一合并
        List<Long> items = mergeVo.getItems();
        //过滤掉不符合采购状态的
        List<Long> satisfyIds = items.stream().filter((id) -> {
            PurchaseDetailEntity detailServiceById = purchaseDetailService.getById(id);
            return detailServiceById.getStatus() == WareConstant.PurchaseDetailEnum.CREATED.getCode() ||
                    detailServiceById.getStatus() == WareConstant.PurchaseDetailEnum.ASSIGNED.getCode();
        }).collect(Collectors.toList());

        Long finalPurchaseId = purchaseId;
        // 所有的采购需求合并成采购单
        List<PurchaseDetailEntity> detailEntities = satisfyIds.stream().map((i) -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(i);
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailEnum.ASSIGNED.getCode());
            return purchaseDetailEntity;
        }).collect(Collectors.toList());

        purchaseDetailService.updateBatchById(detailEntities);

        PurchaseEntity purchaseEntity = new PurchaseEntity();

        purchaseEntity.setId(finalPurchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    @Transactional
    @Override
    public void received(List<Long> purchaseIds) {
        //1.确认当前采购单是新建或者已分配状态
        List<PurchaseEntity> purchaseEntities = purchaseIds.stream().map((id) -> {
            //得到采购单，判断状态
            PurchaseEntity purchaseEntity = this.getById(id);

            return purchaseEntity;
            //过滤非01状态的采购单
        }).filter((item) -> {
            return item.getStatus() == WareConstant.PurchaseEnum.CREATED.getCode() ||
                    item.getStatus() == WareConstant.PurchaseEnum.ASSIGNED.getCode();
            //更新采购单状态
        }).peek((entity) -> {
            entity.setStatus(WareConstant.PurchaseEnum.RECEIVED.getCode());
            entity.setUpdateTime(new Date());
        }).collect(Collectors.toList());


        //2.改变采购单状态
        this.updateBatchById(purchaseEntities);

        //3.改变采购单中采购项状态
        purchaseEntities.forEach((entity) -> {
            //得到当前采购单的所有采购项
            List<PurchaseDetailEntity> detailEntities = purchaseDetailService
                    .listDetailsByPurchaseId(entity.getId());
            //对每个采购项更新状态
            detailEntities.forEach((detailEntitity) -> {
                detailEntitity.setPurchaseId(entity.getId());
                detailEntitity.setStatus(WareConstant.PurchaseDetailEnum.BUYING.getCode());
            });
            purchaseDetailService.updateBatchById(detailEntities);
        });
    }


    @Transactional
    @Override
    public void done(PurchaseCompleteVo completeVo) {


        //2.改变采购项状态
        List<PurchaseItemVo> items = completeVo.getItems();

        List<PurchaseDetailEntity> updates = new ArrayList<>();

        boolean flag = true;

        for (PurchaseItemVo item : items) {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();

            if (item.getStatus() == WareConstant.PurchaseDetailEnum.PURCHASEFAIL.getCode()) {
                flag = false;
                purchaseDetailEntity.setStatus(item.getStatus());
            } else {
                //3.采购项成功后入库
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailEnum.COMPLETED.getCode());
                PurchaseDetailEntity detailEntityById = purchaseDetailService.getById(item.getItemId());
                wareSkuService.addStock(detailEntityById.getSkuId(), detailEntityById.getWareId(), detailEntityById.getSkuNum());

            }

            purchaseDetailEntity.setId(item.getItemId());

            updates.add(purchaseDetailEntity);
        }
        purchaseDetailService.updateBatchById(updates);

        //1.改变采购单状态
        Long id = completeVo.getId();
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        if (flag) {
            purchaseEntity.setStatus(WareConstant.PurchaseEnum.COMPLETED.getCode());
        } else {
            purchaseEntity.setStatus(WareConstant.PurchaseEnum.HASHERROR.getCode());
        }
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }
}