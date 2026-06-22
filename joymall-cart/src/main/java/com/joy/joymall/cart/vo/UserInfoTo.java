package com.joy.joymall.cart.vo;

import lombok.Data;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/9 17:17
 */
@Data
public class UserInfoTo {

    private Long userId;

    private String userKey;

    /**
     * cookie标志位
     */
    private boolean tempUserKey;
}
