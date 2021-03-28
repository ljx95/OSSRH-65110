package com.ljx.permission.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 15:17
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SqlSignature {
    String tableAlias() default "hc";
    String extTableFiled() default "";
}
