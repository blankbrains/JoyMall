package com.joy.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/9 23:01
 */
public class ListConstraintValidator implements ConstraintValidator<ListValue, Integer> {

    private Set<Integer> values = new HashSet<>();

    //初始化方法
    @Override
    public void initialize(ListValue constraintAnnotation) {
        int[] vals = constraintAnnotation.values();
        for (int value : vals) {
            this.values.add(value);
        }
    }

    //判断是否校验成功

    /**
     * @param integer:提交上来的值
     * @param constraintValidatorContext
     * @return
     */
    @Override
    public boolean isValid(Integer integer, ConstraintValidatorContext constraintValidatorContext) {
        return values.contains(integer);
    }
}
