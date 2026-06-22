package com.joy.joymall.order.interceptor;

import com.joy.common.constant.AuthServerConstant;
import com.joy.common.vo.MemberResponseVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/27 14:40
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberResponseVo> loginThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/order/order/status/**", requestURI);
        boolean aliPayed = new AntPathMatcher().match("/payed/notify", requestURI);
        if (match || aliPayed) {
            return true;
        }

        MemberResponseVo attribute = (MemberResponseVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);

        //成功登录
        if (attribute != null) {
            loginThreadLocal.set(attribute);
            return true;
        } else {
            response.sendRedirect("http://auth.joymall.com/login.html");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        // 请求完成后清除 ThreadLocal，防止线程池复用导致数据泄露
        loginThreadLocal.remove();
    }
}
