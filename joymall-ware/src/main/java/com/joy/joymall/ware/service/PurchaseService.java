package com.joy.joymall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.joy.common.utils.PageUtils;
import com.joy.joymall.ware.entity.PurchaseEntity;
import com.joy.joymall.ware.vo.MergeVo;
import com.joy.joymall.ware.vo.purchasevo.PurchaseCompleteVo;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-04 18:29:13
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageUnreceived(Map<String, Object> params);

    void mergePurchase(MergeVo mergeVo);

    void received(List<Long> purchaseIds);

    void done(PurchaseCompleteVo completeVo);
}

