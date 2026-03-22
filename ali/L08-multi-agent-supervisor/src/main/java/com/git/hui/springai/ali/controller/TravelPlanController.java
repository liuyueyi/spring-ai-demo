package com.git.hui.springai.ali.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.git.hui.springai.ali.planer.TravelSupervisorPlanAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

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

    public record PlanInfo(String destination, String days, String budget, String preferences) {
    }

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

        try {
            Agent supervisorAgent = travelSupervisorPlanAgent.agent();

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
                    .addMetadata("destination", destination)
                    .addMetadata("days", days)
                    .addMetadata("budget", budget)
                    .addMetadata("preferences", preferences)
                    .build();

            // 获取流式输出
            Flux<NodeOutput> agentStream = supervisorAgent.stream(prompt, config);

            return agentStream
                    .filter(nodeOutput -> !(nodeOutput instanceof StreamingOutput<?> so &&
                            so.getOutputType() == OutputType.AGENT_MODEL_FINISHED))
                    .map(nodeOutput -> {
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
                            OutputType outputType = streamingOutput.getOutputType();

                            if (outputType == OutputType.AGENT_MODEL_STREAMING) {
                                // 流式消息，增量返回
                                String text = streamingOutput.message().getText();
                                if (text != null && !text.trim().isEmpty()) {
                                    contentBuilder.append(text);
                                    hasContent = true;
                                    data.put("contentType", "delta");
                                }
                            } else if (outputType == OutputType.AGENT_TOOL_FINISHED) {
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

                        log.info("返回数据 - agent: {}, stage: {}, hasContent: {}, contentLength: {}",
                                agentName, stage, hasContent, contentBuilder.length());

                        String json;
                        try {
                            json = objectMapper.writeValueAsString(data);
                        } catch (JsonProcessingException e) {
                            log.error("JSON 序列化失败", e);
                            json = "{\"error\":true,\"errorMessage\":\"JSON 序列化失败\"}";
                        }

                        return ServerSentEvent.<String>builder()
                                .event("message")
                                .data(json)
                                .build();
                    })
                    .onErrorResume(error -> {
                        log.error("流式规划过程中发生错误", error);

                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("error", true);
                        errorData.put("errorType", error.getClass().getSimpleName());
                        errorData.put("errorMessage", error.getMessage() != null ? error.getMessage() : "未知错误");

                        String errorJson;
                        try {
                            errorJson = objectMapper.writeValueAsString(errorData);
                        } catch (JsonProcessingException e) {
                            log.error("错误信息 JSON 序列化失败", e);
                            errorJson = "{\"error\":true,\"errorMessage\":\"JSON 序列化失败\"}";
                        }

                        return Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("error")
                                        .data(errorJson)
                                        .build()
                        );
                    })
                    .doOnComplete(() -> {
                        log.info("流式规划完成");
                    });

        } catch (Exception e) {
            log.error("创建流式接口时发生错误", e);

            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", true);
            errorData.put("errorType", e.getClass().getSimpleName());
            errorData.put("errorMessage", "初始化失败：" + e.getMessage());

            String errorJson;
            try {
                errorJson = objectMapper.writeValueAsString(errorData);
            } catch (JsonProcessingException ex) {
                log.error("错误信息 JSON 序列化失败", ex);
                errorJson = "{\"error\":true,\"errorMessage\":\"JSON 序列化失败\"}";
            }

            return Flux.just(
                    ServerSentEvent.<String>builder()
                            .event("error")
                            .data(errorJson)
                            .build()
            );
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
