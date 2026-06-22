package com.joy.joymall.seckill.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/9/12 21:23
 */
@Data
public class SecKillSessionsWithSkusVo {
    /**
     * id
     */
    private Long id;
    /**
     * 场次名称
     */
    private String name;
    /**
     * 每日开始时间
     */
    private Date startTime;
    /**
     * 每日结束时间
     */
    private Date endTime;
    /**
     * 启用状态
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 当前上架时间点需要上架的商品
     */
    private List<SecKillSkuVo> relationSkus;
}
