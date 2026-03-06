package com.git.hui.springai.mvc;

import com.git.hui.springai.advisor.MyLoggingAdvisor;
import com.git.hui.springai.tools.QuizTools;
import com.git.hui.springai.tools.ToolResponseType;
import com.git.hui.springai.tools.WeatherTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author YiHui
 * @date 2026/3/6
 */
@Slf4j
@RestController
public class QaController {
    private QuizTools quizTools;
    private WeatherTools weatherTools;

    private final List<ToolCallback> tools;
    private final ChatClient chatClient;

    public QaController(QuizTools quizTools, WeatherTools weatherTools, ChatClient.Builder chatClientBuilder) {
        this.quizTools = quizTools;
        this.weatherTools = weatherTools;
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        MyLoggingAdvisor.builder()
                                .showSystemMessage(true).showAvailableTools(true).build())
                .build();

        ToolCallback[] t1 = MethodToolCallbackProvider.builder()
                .toolObjects(quizTools)
                .build()
                .getToolCallbacks();
        ToolCallback[] t2 = MethodToolCallbackProvider.builder()
                .toolObjects(weatherTools)
                .build()
                .getToolCallbacks();
        tools = new ArrayList<>();
        tools.addAll(List.of(t1));
        tools.addAll(List.of(t2));
    }

    @RequestMapping(path = "executeTools")
    public String aiExecuteTools(String msg) {
        // 工具执行上下文
        Map<String, Object> toolContextData = new HashMap<>();
        toolContextData.put("sessionId", "demo-session-123");  // 示例 sessionId
        toolContextData.put("userId", "demo-user-456");        // 示例 userId
        toolContextData.put("timestamp", System.currentTimeMillis());

        // 默认的场景，由SpringAI来控制工具的执行；如果大模型一次性要求调用多个工具，Spring AI 会全部执行后再统一返回给大模型。最终由大模型组装完成的结果返回给用户
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(true)  // 启用自动执行
                .toolContext(toolContextData)
                .build();
        Prompt prompt = new Prompt(msg, options);
        return chatClient.prompt(prompt).toolCallbacks(this.tools).call().content();
    }

    @RequestMapping(path = "qa")
    public String qa(String msg) {
        // 工具执行上下文
        Map<String, Object> toolContextData = new HashMap<>();
        toolContextData.put("sessionId", "demo-session-123");  // 示例 sessionId
        toolContextData.put("userId", "demo-user-456");        // 示例 userId
        toolContextData.put("timestamp", System.currentTimeMillis());

        // 控制手动执行工具
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)  // 禁用自动执行，由我们手动控制
                .toolContext(toolContextData)
                .build();

        // ✅ 第 1 次调用
        Prompt prompt = new Prompt(msg, options);
        ChatResponse chatResponse = chatClient.prompt(prompt).toolCallbacks(this.tools).call().chatResponse();
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
        if (CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
            // 非工具调用结果，将结果添加到上下文
            log.info("非工具调用结果：{}", assistantMessage.getText());
            return assistantMessage.getText();
        }

        // 由我们主动来控制工具的执行(但是需要注意的是，不能有同名的工具，会报错)
        List<ToolResponseMessage.ToolResponse> list = new ArrayList<>();
        for (var call : assistantMessage.getToolCalls()) {
            for (ToolCallback callback : tools) {
                if (callback.getToolDefinition().name().equals(call.name())) {
                    var toolRsp = callback.call(call.arguments(), new ToolContext(toolContextData));
                    ToolResponseMessage.ToolResponse toolResponse =
                            new ToolResponseMessage.ToolResponse(call.id(), call.name(), toolRsp);
                    list.add(toolResponse);

                    if (callback instanceof MethodToolCallback) {
                        var target = ((MethodToolCallback) callback);
                        // 获取 toolMethod
                        Field field = ReflectionUtils.findField(target.getClass(), "toolMethod");
                        field.setAccessible(true);
                        var method = (Method) ReflectionUtils.getField(field, target);
                        if (method != null) {
                            var rspType = method.getDeclaredAnnotation(ToolResponseType.class);
                            log.info("工具方法定义信息：{}", rspType);
                        }
                    }

                    // 我们还可以通过反射的方式，获取工具上通过自定义注解维护的信息
                    log.info("工具定义信息：{}", callback.getToolDefinition().description());
                }
            }
        }


        // ✅ 第 2 次调用：将工具结果返回给大模型，让它总结
        ToolResponseMessage toolMsg = ToolResponseMessage.builder().responses(list).build();

        ChatResponse finalResponse = chatClient.prompt(
                        new Prompt(List.of(
                                new UserMessage(msg),           // 用户原始问题
                                assistantMessage,            // AI 的工具调用请求
                                toolMsg                      // 工具执行结果
                        )))
                .call()
                .chatResponse();

        // 返回大模型的最终总结
        return finalResponse.getResult().getOutput().getText();
    }
}
