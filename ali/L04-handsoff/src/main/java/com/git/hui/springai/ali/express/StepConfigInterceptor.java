package com.git.hui.springai.ali.express;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于步骤配置的拦截器，根据当前步骤动态配置系统提示词和可用工具
 * 
 * @author YiHui
 * @date 2026/3/12
 */
@Slf4j
public class StepConfigInterceptor extends ModelInterceptor {

    private final Map<String, StepConfig> stepConfigMap;

    public StepConfigInterceptor(Map<String, StepConfig> stepConfigMap) {
        this.stepConfigMap = stepConfigMap;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        Map<String, Object> context = request.getContext();
        // 从上下文中获取当前的执行进度
        String currentStep = (String) context.getOrDefault(ExpressOrderStateConstants.KEY_CURRENT_STEP, ExpressOrderStateConstants.STEP_RECEIVE_INFO_COLLECTOR);

        StepConfig stepConfig = stepConfigMap.get(currentStep);
        if (stepConfig == null) {
            log.warn("未知步骤：{}, 使用默认步骤：{}", currentStep, ExpressOrderStateConstants.STEP_RECEIVE_INFO_COLLECTOR);
            stepConfig = stepConfigMap.get(ExpressOrderStateConstants.STEP_RECEIVE_INFO_COLLECTOR);
        }

        // 检查必需的前置条件是否满足
        for (String required : stepConfig.requiredKeys()) {
            if (context.get(required) == null) {
                throw new IllegalStateException("在进入 " + currentStep + " 之前必须先提供：" + required);
            }
        }

        // 格式化提示词，替换上下文中的变量
        String systemPrompt = formatPrompt(stepConfig.prompt(), context);
        
        // 获取本次调用可用的工具列表
        List<String> toolNames = stepConfig.tools().stream()
                .map(t -> t.getToolDefinition().name())
                .collect(Collectors.toList());

        log.info("【快递下单】currentStep = {}, availableTools = {}, prompt = {}, context = {}", currentStep, toolNames, systemPrompt, JSONObject.toJSON(context));

        // 构建新的请求，应用当前步骤的配置
        ModelRequest overridden = ModelRequest.builder(request)
                .systemMessage(new SystemMessage(systemPrompt))
                // 动态筛选工具，指定本次调用可用的工具名称列表
                .tools(toolNames)
//                .dynamicToolCallbacks(stepConfig.tools)
                .build();

        return handler.call(overridden);
    }

    /**
     * 格式化提示词模板，替换上下文中的占位符
     */
    private static String formatPrompt(String template, Map<String, Object> context) {
        String s = template;
        for (Map.Entry<String, Object> e : context.entrySet()) {
            if (e.getValue() != null && !(e.getValue() instanceof List) 
                    && !"messages".equals(e.getKey()) 
                    && !ExpressOrderStateConstants.KEY_ORDER_STEP.equals(e.getKey())) {
                s = s.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
            }
        }
        return s;
    }

    @Override
    public String getName() {
        return "StepConfig";
    }

    /**
     * 步骤配置记录类
     * 
     * @param prompt       该步骤的系统提示词模板
     * @param tools        该步骤可用的工具列表
     * @param requiredKeys 进入该步骤前必须提供的状态键列表
     */
    public record StepConfig(String prompt, List<ToolCallback> tools, List<String> requiredKeys) {
    }
}
