package com.joy.joymall.member.interceptor;

import com.joy.common.constant.AuthServerConstant;
import com.joy.common.vo.MemberResponseVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

        // 仅允许 GET 请求（查询类远程调用）免登录，POST/PUT/DELETE 等写操作需要登录
        String requestURI = request.getRequestURI();
        boolean isReadRequest = "GET".equalsIgnoreCase(request.getMethod())
                && new AntPathMatcher().match("/member/**", requestURI);
        if (isReadRequest) {
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
}
