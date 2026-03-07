package com.git.hui.springai.app.protocol.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 工具响应类型注解
 * 用于声明工具方法的返回内容类型，如 card、quiz、chart 等
 * 
 * @author YiHui
 * @date 2026/3/6
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolResponseType {
    /**
     * 响应类型标识
     * @return 类型字符串，如 "card", "quiz", "chart"
     */
    String value();
}
