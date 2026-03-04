package com.git.hui.springai.app.react.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.List;

/**
 * 简单计算器工具 - 用于演示 ReAct
 */
public class CalculatorTools {
    private static final Logger log = LoggerFactory.getLogger(CalculatorTools.class);

    /**
     * 加法运算
     */
    @Tool(description = "执行加法运算，返回两个数的和")
    public double add(@ToolParam(description = "第一个加数") double a,
                      @ToolParam(description = "第二个加数") double b) {
        log.debug("[🔨] 执行加法：{} + {}", a, b);
        return a + b;
    }

    /**
     * 减法运算
     */
    @Tool(description = "执行减法运算，返回两个数的差")
    public double subtract(@ToolParam(description = "被减数") double a,
                           @ToolParam(description = "减数") double b) {
        log.debug("[🔨] 执行减法：{} - {}", a, b);
        return a - b;
    }

    /**
     * 乘法运算
     */
    @Tool(description = "执行乘法运算，返回两个数的积")
    public double multiply(@ToolParam(description = "第一个乘数") double a,
                           @ToolParam(description = "第二个乘数") double b) {
        log.debug("[🔨] 执行乘法：{} * {}", a, b);
        return a * b;
    }

    /**
     * 除法运算
     */
    @Tool(description = "执行除法运算，返回两个数的商")
    public double divide(@ToolParam(description = "被除数") double a,
                         @ToolParam(description = "除数") double b) {
        log.debug("[🔨] 执行除法：{} / {}", a, b);
        if (b == 0) {
            throw new ArithmeticException("除数不能为零");
        }
        return a / b;
    }


    @Tool(description = "查询天气信息")
    public String weather(@ToolParam(description = "城市名称") String city) {
        log.debug("[🔨] 执行天气查询：{}", city);
        // 随机返回温度信息，有一个列表用于随机取值
        List<String> temperatures = List.of("25°C", "27°C", "23°C", "21°C", "19°C");
        // 天气列表
        List<String> weathers = List.of("晴天", "阴天", "雨天", "雷雨", "雪天");
        // 返回天气 + 温度
        return "当前" + city + "的天气为：" + weathers.get((int) (Math.random() * weathers.size())) + " ,气温为：" + temperatures.get((int) (Math.random() * temperatures.size()));
    }

    /**
     * 获取所有工具回调
     */
    public List<ToolCallback> getTools() {
        ToolCallback[] toolCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(this)
                .build()
                .getToolCallbacks();
        return List.of(toolCallbacks);
    }
}
