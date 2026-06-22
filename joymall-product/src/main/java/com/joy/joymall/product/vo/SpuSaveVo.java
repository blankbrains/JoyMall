/**
 * Copyright 2020 bejson.com
 */
package com.joy.joymall.product.vo;

import com.joy.joymall.product.vo.spuentity.BaseAttrs;
import com.joy.joymall.product.vo.spuentity.Bounds;
import com.joy.joymall.product.vo.spuentity.Skus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Auto-generated: 2020-05-31 11:3:26
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */

@Data
public class SpuSaveVo {

    private String spuName;
    private String spuDescription;
    private Long catalogId;
    private Long brandId;
    private BigDecimal weight;
    private int publishStatus;
    private List<String> decript;
    private List<String> images;
    private Bounds bounds;
    private List<BaseAttrs> baseAttrs;
    private List<Skus> skus;

}
