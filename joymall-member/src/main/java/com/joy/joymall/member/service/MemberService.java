package com.joy.joymall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.joy.common.utils.PageUtils;
import com.joy.joymall.member.entity.MemberEntity;
import com.joy.joymall.member.exception.PhoneAlreadyExistException;
import com.joy.joymall.member.exception.UserNameAlreadyExistException;
import com.joy.joymall.member.vo.MemberLoginVo;
import com.joy.joymall.member.vo.MemberRegisterVo;
import com.joy.joymall.member.vo.SocialUserVo;

import java.util.Map;

/**
 * 会员
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-04 18:07:36
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(MemberRegisterVo vo);

    void checkMobileUnique(String phone) throws PhoneAlreadyExistException;

    void checkUserNameUnique(String userName) throws UserNameAlreadyExistException;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUserVo vo) throws Exception;
}

