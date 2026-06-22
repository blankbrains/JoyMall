package com.joy.joymall.member.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/5 17:08
 */
@Data
public class MemberRegisterVo {


    private String userName;


    private String password;


    private String phone;
}
