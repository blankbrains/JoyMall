package com.joy.joymall.search.service;


import com.joy.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/25 0:01
 */


public interface ProductSaveService {


    void productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}
