package com.git.hui.springai.ali.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.git.hui.springai.ali.context.PlanContext;
import com.git.hui.springai.ali.planer.TravelSupervisorPlanAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 旅游行程规划控制器 - 提供流式对话接口
 *
 * @author YiHui
 * @date 2026/3/22
 */
@Slf4j
@RestController
@RequestMapping("/api/travel")
@RequiredArgsConstructor
public class TravelPlanController {
    private final TravelSupervisorPlanAgent travelSupervisorPlanAgent;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 流式旅游规划接口
     * 使用 SSE (Server-Sent Events) 实时返回规划进度
     *
     * @param destination 目的地
     * @param days 旅行天数
     * @param budget 预算（可选）
     * @param preferences 偏好（可选，如：美食、购物、自然风光等）
     * @return 流式响应
     */
    @GetMapping(value = "/plan", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> planTravelStream(
            @RequestParam String destination,
            @RequestParam(required = false, defaultValue = "3") Integer days,
            @RequestParam(required = false) String budget,
            @RequestParam(required = false) String preferences) {

        log.info("开始规划旅行：目的地={}, 天数={}, 预算={}, 偏好={}",
                destination, days, budget, preferences);
        String sessionId = UUID.randomUUID().toString();

        try {
            Agent supervisorAgent = travelSupervisorPlanAgent.agent(sessionId);

            // 构建用户请求
            StringBuilder userRequest = new StringBuilder();
            userRequest.append("我想去").append(destination).append("旅行");
            if (days != null && days > 0) {
                userRequest.append(days).append("天");
            }
            if (budget != null && !budget.trim().isEmpty()) {
                userRequest.append("，预算").append(budget);
            }
            if (preferences != null && !preferences.trim().isEmpty()) {
                userRequest.append("，我喜欢").append(preferences);
            }
            userRequest.append("。请帮我制定详细的旅行计划。");

            String prompt = userRequest.toString();

            RunnableConfig config = RunnableConfig.builder()
                    .threadId("1")
                    .addMetadata("sessionId", sessionId)
                    .addMetadata("destination", destination)
                    .addMetadata("days", days)
                    .addMetadata("budget", budget)
                    .addMetadata("preferences", preferences)
                    .build();

            // 获取流式输出
            Flux<NodeOutput> agentStream = supervisorAgent.stream(prompt, config);

            return Flux.create(sink -> {
                PlanContext.set(sink);
                PlanContext.setSessionEmitter(sessionId, sink);
                System.out.println("初始化Sink");
                CompletableFuture.runAsync(() -> {
                    agentStream.filter(nodeOutput -> !(nodeOutput instanceof StreamingOutput<?> so && so.getOutputType() == OutputType.AGENT_MODEL_FINISHED))
                            .subscribe(
                                    nodeOutput -> {
                                        String node = nodeOutput.node();
                                        String agentName = nodeOutput.agent();

                                        log.info("收到节点输出 - node: {}, agent: {}", node, agentName);

                                        // 构建响应数据
                                        Map<String, Object> data = new HashMap<>();
                                        data.put("node", node);
                                        data.put("agent", agentName);

                                        StringBuilder contentBuilder = new StringBuilder();
                                        boolean hasContent = false;

                                        // 提取内容
                                        if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
                                            Message message = streamingOutput.message();
                                            log.info("节点类型 - node: {} type: {}", node, streamingOutput.getOutputType());

                                            if (message instanceof AssistantMessage assistantMessage) {
                                                // 检查是否有工具调用
                                                if (assistantMessage.hasToolCalls()) {
                                                    // 工具调用完成
                                                    data.put("contentType", "tool_execute");
                                                    var toolCalls = assistantMessage.getToolCalls().get(0);
                                                    contentBuilder.append("\n[工具执行]：")
                                                            .append(toolCalls.id())
                                                            .append(":")
                                                            .append(toolCalls.name())
                                                            .append(" => args => ")
                                                            .append(toolCalls.arguments());
                                                    hasContent = true;
                                                } else {
                                                    // 流式消息，增量返回
                                                    String text = streamingOutput.message().getText();
                                                    if (text != null && !text.trim().isEmpty()) {
                                                        contentBuilder.append(text);
                                                        hasContent = true;
                                                        data.put("contentType", "delta");
                                                    }
                                                }
                                            } else if (message instanceof ToolResponseMessage) {
                                                // 工具调用完成
                                                data.put("contentType", "tool_complete");
                                                contentBuilder.append("[工具执行完成]");
                                                hasContent = true;
                                            }
                                        }

                                        data.put("content", contentBuilder.toString());
                                        data.put("hasContent", hasContent);

                                        // 根据 agent 类型确定阶段标识
                                        String stage = determineStage(agentName);
                                        data.put("stage", stage);

                                        log.info("返回数据 - agent: {}, stage: {}, hasContent: {}, contentLength: {}", agentName, stage, hasContent, contentBuilder.length());

                                        String json = toJsonStr(data);
                                        sink.next(ServerSentEvent.<String>builder()
                                                .event("message")
                                                .data(json)
                                                .build());
                                    },
                                    (error) -> {
                                        log.error("流式规划过程中发生错误", error);
                                        sink.next(buildErrorEvent(error));
                                    },
                                    () -> {
                                        log.info("规划完成");
                                        PlanContext.remove();
                                        PlanContext.removeSessionEmitter(sessionId);
                                        System.out.println("取消上下文");
                                    });
                });
                sink.onCancel(() -> log.info("客户端取消流式接口"));
            });
        } catch (Exception e) {
            log.error("创建流式接口时发生错误", e);
            return Flux.just(buildErrorEvent(e));
        }
    }

    private ServerSentEvent<String> buildErrorEvent(Throwable e) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error", true);
        errorData.put("errorType", e.getClass().getSimpleName());
        errorData.put("errorMessage", e.getMessage() != null ? "异常：" + e.getMessage() : "未知错误");

        String errorJson = toJsonStr(errorData);
        return ServerSentEvent.<String>builder()
                .event("error")
                .data(errorJson)
                .build();
    }

    private String toJsonStr(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("错误信息 JSON 序列化失败", e);
            return "{\"error\":true,\"errorMessage\":\"JSON 序列化失败\"}";
        }
    }

    /**
     * 根据 agent 名称确定规划阶段
     */
    private String determineStage(String agentName) {
        if (agentName != null) {
            if (agentName.contains("attraction")) {
                return "attraction"; // 景点推荐
            } else if (agentName.contains("hotel")) {
                return "hotel"; // 住宿推荐
            } else if (agentName.contains("transport")) {
                return "transport"; // 交通规划
            } else if (agentName.contains("supervisor")) {
                return "supervisor"; // 监督者统筹
            }
        }
        return "unknown";
    }
}
