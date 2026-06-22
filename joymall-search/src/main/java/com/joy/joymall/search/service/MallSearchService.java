package com.joy.joymall.search.service;

import com.joy.joymall.search.vo.SearchParam;
import com.joy.joymall.search.vo.SearchResult;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/29 23:37
 */
public interface MallSearchService {
    SearchResult search(SearchParam searchParam);
}
