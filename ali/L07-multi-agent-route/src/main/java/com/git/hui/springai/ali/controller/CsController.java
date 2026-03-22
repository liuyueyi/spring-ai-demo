package com.git.hui.springai.ali.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.git.hui.springai.ali.cs.CsRouterAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
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
 * 智能客服控制器 - 提供流式对话接口
 *
 * @author YiHui
 * @date 2026/3/21
 */
@Slf4j
@RestController
@RequestMapping("/api/cs")
public class CsController {
    private final LlmRoutingAgent routerAgent;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CsController(CsRouterAgent routerAgent) {
        this.routerAgent = routerAgent.routerAgent();
    }

    /**
     * 流式智能客服接口
     * 使用 SSE (Server-Sent Events) 实时返回对话内容，自动路由到对应的业务 agent
     *
     * @param message 用户消息
     * @return 流式响应
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestParam String message) {
        log.info("收到客服消息：{}", message);

        try {
            // 获取流式输出
            Flux<NodeOutput> agentStream = routerAgent.stream(message);

            return agentStream
                    .filter(nodeOutput -> !(nodeOutput instanceof StreamingOutput<?> so &&
                            so.getOutputType() == OutputType.AGENT_MODEL_FINISHED))
                    .map(nodeOutput -> {
                        String node = nodeOutput.node();
                        String agentName = nodeOutput.agent();

                        // 构建响应数据
                        Map<String, Object> data = new HashMap<>();
                        data.put("node", node);
                        data.put("agent", agentName);

                        StringBuilder contentBuilder = new StringBuilder();
                        boolean hasContent = false;

                        if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
                            Message msg = streamingOutput.message();
                            if (msg instanceof AssistantMessage assistantMessage) {
                                if (!assistantMessage.hasToolCalls()) {
                                    String text = assistantMessage.getText();
                                    if (text != null && !text.trim().isEmpty()) {
                                        contentBuilder.append(text);
                                        hasContent = true;
                                    }
                                }
                            }
                        }

                        data.put("content", contentBuilder.toString());
                        data.put("hasContent", hasContent);

                        // 根据 agent 类型设置服务部门标识
                        String department = determineDepartment(agentName);
                        data.put("department", department);

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
                        log.error("流式对话过程中发生错误", error);

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
                        log.info("流式对话完成");
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
     * 根据 agent 名称确定服务部门
     */
    private String determineDepartment(String agentName) {
        if (agentName != null) {
            if (agentName.contains("sales")) {
                return "sales"; // 销售部门
            } else if (agentName.contains("hr")) {
                return "hr"; // 人力资源部门
            } else if (agentName.contains("tech")) {
                return "tech"; // 技术支持部门
            } else if (agentName.contains("router")) {
                return "router"; // 路由中心
            }
        }
        return "unknown";
    }
}
