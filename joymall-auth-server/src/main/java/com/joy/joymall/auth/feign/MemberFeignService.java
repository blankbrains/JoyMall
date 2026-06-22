package com.joy.joymall.auth.feign;

import com.joy.common.utils.R;
import com.joy.joymall.auth.vo.SocialUserVo;
import com.joy.joymall.auth.vo.UserLoginVo;
import com.joy.joymall.auth.vo.UserRegisterVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/6 20:15
 */
@FeignClient("joymall-member")
public interface MemberFeignService {
    @PostMapping("/member/member/register")
    R register(@RequestBody UserRegisterVo vo);


    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/oauth2/login")
    R oauthLogin(@RequestBody SocialUserVo vo) throws Exception;

}
