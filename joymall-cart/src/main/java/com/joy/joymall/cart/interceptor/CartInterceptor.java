package com.joy.joymall.cart.interceptor;

import com.joy.common.constant.AuthServerConstant;
import com.joy.common.constant.CartConstant;
import com.joy.common.vo.MemberResponseVo;
import com.joy.joymall.cart.vo.UserInfoTo;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.UUID;

/**
 * @Description: 在执行目标方法之前，都需要判断用户的登录状态
 * @Author:joymall
 * @Date:2022/6/9 17:14
 */
@Component
public class CartInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        //判断用户登没登陆
        HttpSession session = request.getSession();
        MemberResponseVo attribute = (MemberResponseVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        UserInfoTo userInfoTo = new UserInfoTo();
        if (attribute != null) {
            //用户登录
            userInfoTo.setUserId(attribute.getId());
        }
        //用户没登录会有一个user-key
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                if (name.equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
                    userInfoTo.setUserKey(cookie.getValue());
                }
            }
        }

        //无临时用户
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
            userInfoTo.setTempUserKey(true);
        }
        //目标方法执行之前，让此数据在当前线程共享
        threadLocal.set(userInfoTo);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) throws Exception {

        UserInfoTo userInfoTo = threadLocal.get();
        if (userInfoTo.isTempUserKey()) {
            //请求执行完如果没有设置过cookie的话，让浏览器保存一个cookie，为时一个月
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, threadLocal.get().getUserKey());
            cookie.setDomain("joymall.com");
            cookie.setMaxAge(CartConstant.COOKIE_EXPIRE_TIME);
            response.addCookie(cookie);
        }
    }
}
