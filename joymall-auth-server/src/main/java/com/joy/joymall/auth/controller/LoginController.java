package com.joy.joymall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.joy.common.constant.AuthServerConstant;
import com.joy.common.exception.BizCodeEnum;
import com.joy.common.utils.R;
import com.joy.common.vo.MemberResponseVo;
import com.joy.joymall.auth.feign.MemberFeignService;
import com.joy.joymall.auth.feign.ThirdPartyFeignService;
import com.joy.joymall.auth.vo.UserLoginVo;
import com.joy.joymall.auth.vo.UserRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/4 18:56
 */
@Controller
public class LoginController {

    @Autowired
    private ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberFeignService memberFeignService;



    /**
     * 验证码校验与发送
     *
     * @param phone
     * @return
     */
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(String phone) {
        //TODO 接口防刷
        //先判断验证码有没有过期
        String existCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        long redisTime = Integer.MAX_VALUE;
        if (existCode != null) {

            redisTime = Long.parseLong(existCode.split("_")[1]);
        }
        //60s内不允许再次发送
        if (System.currentTimeMillis() - redisTime < 60 * 1000) {
            return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
        }
        //先存入redis,key=sms:code:手机号  value=验证码
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 5);
        //存入redis，设置5分钟过期
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone,
                code + "_" + System.currentTimeMillis(), 5, TimeUnit.MINUTES);
        thirdPartyFeignService.sendCode(phone, code);
        return R.ok();
    }


    /**
     * 用户注册
     * TODO 解决分布式session数据不共享
     *
     * @param userRegisterVo ：用户传入结果
     * @param result         ： 数据校验结果（格式校验）
     * @return ;
     */
    @PostMapping("/register")
    public String register(@Valid UserRegisterVo userRegisterVo, BindingResult result,
                           RedirectAttributes attributes) {


        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

            attributes.addFlashAttribute("errors", errors);
            //此处不能转发，因为当前请求方式为Post，但是转发的请求是Get，会导致请求不允许
            return "redirect:http://auth.joymall.com/register.html";
        }
        String phone = userRegisterVo.getPhone();
        String code = userRegisterVo.getCode();
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)) {
            String realCode = redisCode.split("_")[0];
            if (code.equals(realCode)) {
                //验证码对比成功,发送远程服务进行真实注册
                R r = memberFeignService.register(userRegisterVo);
                if (r.getCode() == 0) {
                    //成功
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", r.getMessage());
                    attributes.addFlashAttribute("errors", errors);
                    //注册完成后删除验证码，令牌机制
                    redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
                    return "redirect:http://auth.joymall.com/login.html";
                } else {
                    return "redirect:http://auth.joymall.com/register.html";
                }
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                attributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.joymall.com/register.html";
            }
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码失效");
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.joymall.com/register.html";
        }
    }


    /**
     * 普通登录
     * @param vo
     * @param attributes
     * @param session
     * @return
     */
    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes attributes, HttpSession session) {
        R r = memberFeignService.login(vo);
        if (r.getCode() == 0) {
            MemberResponseVo data = r.getData(new TypeReference<MemberResponseVo>() {
            }, "data");
            session.setAttribute(AuthServerConstant.LOGIN_USER, data);
            return "redirect:http://joymall.com";
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", r.getMessage());
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.joymall.com/login.html";
        }
    }


    /**
     * 判断是否登录过
     * @param session
     * @return
     */
    @GetMapping("/login.html")
    public String loginPage(HttpSession session) {
        Object loginUser = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (loginUser == null) {

            return "login";
        } else {
            return "redirect:http://joymall.com";
        }
    }
}
