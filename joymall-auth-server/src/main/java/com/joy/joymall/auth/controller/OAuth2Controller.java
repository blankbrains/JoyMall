package com.joy.joymall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.joy.common.constant.AuthServerConstant;
import com.joy.common.utils.HttpUtils;
import com.joy.common.utils.R;
import com.joy.common.vo.MemberResponseVo;
import com.joy.joymall.auth.feign.MemberFeignService;
import com.joy.joymall.auth.vo.SocialUserVo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 社交登录请求
 * @Author:joymall
 * @Date:2022/6/6 23:51
 */
@Controller
public class OAuth2Controller {

    @Autowired
    private MemberFeignService memberFeignService;

    @Value("${gitee.client-id:your-client-id}")
    private String giteeClientId;

    @Value("${gitee.client-secret:your-client-secret}")
    private String giteeClientSecret;

    @Value("${gitee.redirect-uri:http://auth.joymall.com/oauth2.0/gitee/success}")
    private String giteeRedirectUri;

    @GetMapping("/oauth2.0/gitee/success")
    public String gitee(@RequestParam String code, HttpSession session) throws Exception {
        //根据code码换取accessToken
        Map<String, String> requestParam = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        requestParam.put("grant_type", "authorization_code");
        requestParam.put("code", code);
        requestParam.put("client_id", giteeClientId);
        requestParam.put("redirect_uri", giteeRedirectUri);
        requestParam.put("client_secret", giteeClientSecret);
        HttpResponse response = HttpUtils.doPost("https://gitee.com", "/oauth/token", "post",
                header, requestParam, new HashMap<>());

        //响应成功
        //响应实体转json，再转回自己的实体
        if (response.getStatusLine().getStatusCode() == 200) {

            String jsonEntity = EntityUtils.toString(response.getEntity());
            SocialUserVo socialUserVo = JSON.parseObject(jsonEntity, SocialUserVo.class);
            //保存成功后，自动注册到会员，以后本账户就对应注册账户
            //调用远程服务，来判断当前用户是登录还是注册
            R r = memberFeignService.oauthLogin(socialUserVo);
            if (r.getCode() == 0) {
                TypeReference<MemberResponseVo> typeReference = new TypeReference<MemberResponseVo>() {
                };
                MemberResponseVo data = r.getData(typeReference, "data");
                //解决子域session共享问题
                //使用JSON的序列化方法来序列化到redis中，不然每个对象都要实现序列化接口
                session.setAttribute(AuthServerConstant.LOGIN_USER, data);
                return "redirect:http://joymall.com";
            } else {
                return "redirect:http://auth.joymall.com/login.html";
            }
        } else {
            return "redirect:http://auth.joymall.com/login.html";
        }
    }
}
