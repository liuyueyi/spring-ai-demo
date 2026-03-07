package com.git.hui.springai.app.protocol.service;

import com.git.hui.springai.app.advisor.MyLoggingAdvisor;
import com.git.hui.springai.app.protocol.dto.AgentChatRequest;
import com.git.hui.springai.app.protocol.dto.NdjsonEvent;
import com.git.hui.springai.app.protocol.tool.BarChartCard;
import com.git.hui.springai.app.protocol.tool.ChatTools;
import com.git.hui.springai.app.protocol.tool.QuizCard;
import com.git.hui.springai.app.protocol.tool.ToolResponseType;
import com.git.hui.springai.app.protocol.util.NdjsonEventBuilder;
import com.git.hui.springai.app.protocol.util.RspExtractor;
import io.micrometer.common.KeyValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 聊天服务（对齐 Copilot 协议）
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Service
public class AgentChatService {
    private static final Logger log = LoggerFactory.getLogger(AgentChatService.class);

    private final ChatClient chatClient;
    private final List<ToolCallback> tools;
    private final ChatTools chatTools;  // 用于反射获取工具方法的注解


    // 会话存储（生产环境建议使用 Redis 等）
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public AgentChatService(ChatModel chatModel, ChatTools copilotTools) {
        this.chatTools = copilotTools;
        this.tools = copilotTools.getTools();
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        // Custom logging advisor
                        MyLoggingAdvisor.builder()
                                .showAvailableTools(true)
                                .showSystemMessage(true)
                                .build())
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallRecord {
        private String name;
        private Object data;
    }

    /**
     * 流式聊天（返回 NDJSON 事件流）
     * 使用 Function Calling 机制，让大模型自主决定是否调用工具以及调用哪个工具
     */
    public Flux<NdjsonEvent> streamChat(AgentChatRequest request) {
        String sessionId = getOrCreateSessionId(request.getSessionId());
        String requestId = request.getMetadata() != null ? request.getMetadata().getRequestId() : null;
        String traceId = UUID.randomUUID().toString();
        request.getMetadata().setTraceId(traceId);

        // 1. 发送 session.start 事件
        boolean continueChat = Objects.equals(sessionId, request.getSessionId());
        Flux<NdjsonEvent> startEvent = continueChat ? Flux.empty() :
                Flux.just(NdjsonEventBuilder.createSessionStartEvent(sessionId, requestId, traceId));


        // 2. 构建增强的 Prompt，引导大模型使用可用工具
        String prompt = buildEnhancedPrompt(request.getMessage());
        StringBuilder context = new StringBuilder();
        String messageId = UUID.randomUUID().toString();

        // 用于捕获工具调用结果
        ToolCallRecord toolCallInfo = new ToolCallRecord();

        // 3. 使用支持 Function Calling 的流式处理
        Flux<NdjsonEvent> messageEvents = processMessageWithFunctionCalling(request, sessionId, messageId, prompt, context, toolCallInfo);

        // 4. 消息完成事件
        Flux<NdjsonEvent> completeEvent = Flux.defer(() -> {
            context.append(" [END]");
            return Flux.just(NdjsonEventBuilder.createMessageCompleteEvent(messageId, sessionId, requestId, traceId));
        });

        // 5. 工具调用结果事件（如果有，返回不同的 ui 数据）
        // 说明：工具调用之后我们直接将结果返回给前端对话框，而不是返回给大模型针对工具执行的结果进行综合返回
        // 如果需要将工具的执行结果发送给大模型，那么下面这里还需要再次发起一次大模型调用
        Flux<NdjsonEvent> uiEvent = Flux.defer(() -> {
            if (toolCallInfo.name != null) {
                // 大模型调用了工具，返回工具结果
                return Flux.just(NdjsonEventBuilder.createUiEvent(messageId, toolCallInfo.name, toolCallInfo.data, sessionId, requestId, traceId));
            }
            return Flux.empty();
        });

        // 6. status.waiting_user 事件
        Flux<NdjsonEvent> waitingEvent = Flux.just(NdjsonEventBuilder.createStatusWaitingUserEvent(sessionId, requestId, traceId));

        // 合并所有事件流
        return startEvent
                .concatWith(messageEvents)
                .concatWith(completeEvent)
                .concatWith(uiEvent)
                .concatWith(waitingEvent)
                .onErrorResume(error -> {
                    log.error("Stream chat error", error);
                    return Flux.just(NdjsonEventBuilder.createErrorEvent(error.getMessage(), traceId, requestId, sessionId));
                });
    }


    /**
     * 获取会话详情
     */
    public Map<String, Object> getSession(String sessionId) {
        SessionData session = sessions.get(sessionId);
        if (session == null) {
            return createErrorResponse("SESSION_NOT_FOUND", "Session not found", 404);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("id", session.id);
        sessionData.put("title", session.title);
        sessionData.put("createdAt", session.createdAt);
        sessionData.put("updatedAt", session.updatedAt);
        sessionData.put("status", session.status);

        response.put("session", sessionData);
        response.put("meta", NdjsonEventBuilder.createMetaMap(sessionId, null, null));

        return response;
    }

    /**
     * 获取所有会话列表
     */
    public Map<String, Object> getAllSessions() {
        List<Map<String, Object>> items = new ArrayList<>();

        sessions.values().stream()
                .sorted((a, b) -> b.updatedAt.compareTo(a.updatedAt))
                .forEach(session -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", session.id);
                    item.put("title", session.title);
                    item.put("createdAt", session.createdAt);
                    item.put("updatedAt", session.updatedAt);
                    item.put("status", session.status);
                    items.add(item);
                });

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("items", items);
        response.put("meta", NdjsonEventBuilder.createMetaMap(null, null, null));

        return response;
    }


    /**
     * 结束会话
     * @param sessionId
     * @return
     */
    public Flux<NdjsonEvent> endSession(String sessionId) {
        SessionData sessionData = sessions.remove(sessionId);
        if (sessionData != null) {
            // 7. session.end 事件
            return Flux.just(NdjsonEventBuilder.createSessionEndEvent(sessionId, null, UUID.randomUUID().toString()));
        } else {
            return Flux.empty();
        }
    }

    // ========== 内部方法 ==========

    /**
     * 构建增强的 Prompt，告知大模型可用的工具
     */
    private String buildEnhancedPrompt(String message) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(message);
        prompt.append("\n\n### 可用工具 ###");
        prompt.append("\n你可以使用以下工具来帮助用户：");
        prompt.append("\n1. queryWeather(city) - 查询天气信息，当用户询问天气时调用");
        prompt.append("\n2. createQuiz(topic) - 创建知识问答题目，当用户想要答题或测试时调用");
        prompt.append("\n3. compareData(title, categories, data) - 创建数据对比图表，当用户需要对比数据时调用");
        prompt.append("\n\n如果需要使用工具，请直接调用相应的函数。");
        return prompt.toString();
    }

    /**
     * 使用 Function Calling 处理消息
     * 让大模型自主决定是否调用工具以及调用哪个工具
     */
    private Flux<NdjsonEvent> processMessageWithFunctionCalling(AgentChatRequest request, String sessionId,
                                                                String messageId, String msg,
                                                                StringBuilder context,
                                                                final ToolCallRecord toolCallInfo) {

        // 设置工具调用选项
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)  // 禁用自动执行，由我们手动控制
                .build();
        ToolContext toolContextData = new ToolContext(Map.of("sessionId", sessionId, "userId", "demo1"));
        Prompt prompt = new Prompt(msg, options);
        return chatClient.prompt(prompt)
                .toolCallbacks(tools)
                .stream()
                .chatResponse()
                .doOnNext(content -> {
                    AssistantMessage assistantMessage = content.getResult().getOutput();
                    if (CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
                        // 非工具调用结果，将结果添加到上下文
                        context.append(assistantMessage.getText());
                    } else {
                        // 工具调用
                        var toolRsp = executeTools(assistantMessage, toolContextData);
                        toolCallInfo.name = toolRsp.getKey();
                        toolCallInfo.data = toolRsp.getValue();

                    }
                })
                .map(s -> NdjsonEventBuilder
                        .createMessageDeltaEvent(messageId, s.getResult().getOutput().getText(), sessionId, request.getMetadata().getRequestId(), request.getMetadata().getTraceId())
                );
    }

    /**
     * Act & Observe 阶段 - 执行工具并观察结果
     */
    private KeyValue executeTools(AssistantMessage message, ToolContext toolContext) {
        var toolCall = message.getToolCalls().get(0);  // 只处理第一个工具调用
        // 查找并执行匹配的工具
        for (ToolCallback tool : tools) {
            if (tool.getToolDefinition().name().equals(toolCall.name())) {
                // ✅ 使用带 ToolContext 的 call 方法
                var result = tool.call(toolCall.arguments(), toolContext);
                log.info("【工具】执行返回：{}", result);

                // 从 ToolResponseType 注解中获取响应类型标识
                // 优先从 ToolMetadata 中获取（如果有的话），否则从注解获取
                String responseType = getResponseTypeFromTool(tool, toolCall.name());
                log.info("【工具】响应类型：{} -> {}", toolCall.name(), responseType);

                return KeyValue.of(responseType != null ? responseType : toolCall.name(), result);
            }
        }

        throw new RuntimeException("未找到工具：" + toolCall.name());
    }

    /**
     * 从工具回调中获取响应类型标识
     * 优先从 ToolMetadata 获取，其次从@ToolResponseType 注解获取
     *
     * @param tool 工具回调
     * @param toolName 工具名称
     * @return 响应类型 (card/quiz/chart)，如果没有则返回 null
     */
    private String getResponseTypeFromTool(ToolCallback tool, String toolName) {
        // 尝试从 ToolMetadata 中获取（如果 Spring AI 版本支持）
        try {
            var metadata = tool.getToolMetadata();
            if (metadata != null) {
                // 检查是否有自定义的 responseType 属性
                // 这取决于具体的 ToolMetadata 实现
                log.debug("ToolMetadata: {}", metadata);
            }
        } catch (Exception e) {
            log.debug("无法从 ToolMetadata 获取信息：{}", e.getMessage());
        }

        // 从@ToolResponseType 注解中获取
        return getResponseTypeFromAnnotation(toolName);
    }

    /**
     * 从 @ToolResponseType 注解中获取响应类型标识
     * 通过遍历所有工具对象的方法来获取注解信息
     *
     * @param toolName 工具名称
     * @return 响应类型 (card/quiz/chart)，如果没有则返回 null
     */
    private String getResponseTypeFromAnnotation(String toolName) {
        try {
            // 遍历 CopilotTools 类的所有方法，查找匹配的工具方法
            for (java.lang.reflect.Method method : chatTools.getClass().getDeclaredMethods()) {
                // 检查方法是否有 @ToolResponseType 注解
                if (method.isAnnotationPresent(ToolResponseType.class)) {
                    ToolResponseType annotation = method.getAnnotation(ToolResponseType.class);
                    // 如果方法名与工具名匹配，返回注解中声明的响应类型
                    if (method.getName().equals(toolName)) {
                        return annotation.value();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("无法从@ToolResponseType 注解中获取响应类型：{}", e.getMessage());
        }
        return null;
    }

    private Object parseStructuredData(String content) {
        return RspExtractor.extractJson(content);
    }

    private String getOrCreateSessionId(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            return sessionId;
        }

        String newSessionId = UUID.randomUUID().toString();
        SessionData session = new SessionData();
        session.id = newSessionId;
        session.title = "新对话";
        session.createdAt = Instant.now().toString();
        session.updatedAt = Instant.now().toString();
        session.status = "active";

        sessions.put(newSessionId, session);
        return newSessionId;
    }


    private Map<String, Object> createErrorResponse(String code, String message, int status) {
        Map<String, Object> response = new HashMap<>();
        response.put("ok", false);

        Map<String, Object> error = new HashMap<>();
        error.put("status", status);
        error.put("code", code);
        error.put("message", message);
        error.put("retryable", status >= 500);

        response.put("error", error);
        response.put("meta", NdjsonEventBuilder.createMetaMap(null, null, UUID.randomUUID().toString()));

        return response;
    }

    // ========== 数据结构 ==========

    static class SessionData {
        String id;
        String title;
        String createdAt;
        String updatedAt;
        String status; // "active" | "ended"
    }
}
