package com.joy.joymall.member.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Description: 社交用户信息
 * @author: zhangshuaiyin
 * @createTime: 2021-04-22 19:07
 **/

@Data
public class SocialUserVo implements Serializable {

    private String access_token;

    private String remind_in;

    private long expires_in;

    private String uid;

    private String isRealName;

}