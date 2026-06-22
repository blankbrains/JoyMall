package com.joy.joymall.product.exception;

import com.joy.common.exception.BizCodeEnum;
import com.joy.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 集中处理所有异常
 * @Author:joymall
 * @Date:2022/5/9 21:47
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.joy.joymall.product.controller")
public class JoyMallExceptionControllerAdvice {

    //精确异常
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handlerValidException(MethodArgumentNotValidException e) {
        log.error("数据校验异常:{},异常类型：{}", e.getMessage(), e.getClass());
        BindingResult bindingResult = e.getBindingResult();

        Map<String, String> errorMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach((item) -> {
            errorMap.put(item.getField(), item.getDefaultMessage());
        });
        return R.error(BizCodeEnum.VALID_EXCEPTION.getCode(),BizCodeEnum.VALID_EXCEPTION.getMsg()).put("data", errorMap);
    }


    //其他异常
    @ExceptionHandler(value = Throwable.class)
    public R handlerException(Throwable throwable) {
        log.error("异常信息{}",throwable);
        return R.error(BizCodeEnum.UNKNOWN_EXCEPTION.getCode(),BizCodeEnum.UNKNOWN_EXCEPTION.getMsg());
    }
}
