package com.joy.joymall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.joy.common.utils.R;
import com.joy.joymall.ware.feign.MemberFeignService;
import com.joy.joymall.ware.vo.FareVo;
import com.joy.joymall.ware.vo.MemberReceiveAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.ware.dao.WareInfoDao;
import com.joy.joymall.ware.entity.WareInfoEntity;
import com.joy.joymall.ware.service.WareInfoService;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    private MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                new QueryWrapper<WareInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<WareInfoEntity> queryWrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.eq("id", key)
                    .or().like("name", key)
                    .or().like("address", key)
                    .or().eq("areacode", key);
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 根据收货地址计算运费
     *
     * @param addrId
     * @return
     */
    @Override
    public FareVo getfreight(Long addrId) {
        FareVo fareVo = new FareVo();
        if(ObjectUtils.isEmpty(addrId)){
            addrId = 1L;
        }
        R info = memberFeignService.info(addrId);
        MemberReceiveAddressVo receiveAddressVo = info.getData(new TypeReference<MemberReceiveAddressVo>() {
        }, "memberReceiveAddress");
        if (receiveAddressVo != null) {
            String phone = receiveAddressVo.getPhone();
            String price = phone.substring(phone.length() - 1);
            fareVo.setFare(new BigDecimal(price));
            fareVo.setMemberReceiveAddressVo(receiveAddressVo);
        }
        return fareVo;
    }

}