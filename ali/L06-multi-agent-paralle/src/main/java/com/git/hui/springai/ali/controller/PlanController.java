package com.git.hui.springai.ali.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.git.hui.springai.ali.planer.ParallelPlanAgent;
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
 * 文章创作控制器 - 提供流式 SSE 接口
 *
 * @author YiHui
 * @date 2026/3/17
 */
@Slf4j
@RestController
@RequestMapping("/api/writer")
public class PlanController {

    private final Agent planAgent;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlanController(ParallelPlanAgent planAgent) {
        this.planAgent = planAgent.seqPlanAgent();
    }

    /**
     * 流式创作接口
     * 使用 SSE (Server-Sent Events) 实时返回创作进度
     *
     * @param topic 文章主题
     * @return 流式响应
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> createArticleStream(@RequestParam String topic) {
        log.info("开始创作主题：{}", topic);

        try {
            // 获取流式输出
            Flux<NodeOutput> agentStream = planAgent.stream(topic);

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

                        if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
                            Message message = streamingOutput.message();
                            if (message instanceof AssistantMessage assistantMessage) {
                                if (!assistantMessage.hasToolCalls()) {
                                    String text = assistantMessage.getText();
                                    if (text != null && !text.trim().isEmpty()) {
                                        contentBuilder.append(text);
                                        hasContent = true;
                                        log.info("提取到内容：{}", text.substring(0, Math.min(50, text.length())));
                                    }
                                } else {
                                    log.info("检测到工具调用：{}", assistantMessage.getToolCalls());
                                }
                            } else {
                                log.info("消息类型不是 AssistantMessage: {}", message != null ? message.getClass() : "null");
                                if (agentName.equals(ParallelPlanAgent.PLAN_AGENT)) {
                                    // 表明是 PlanAgent 的输出，将结果输出给前端
                                    hasContent = true;
                                    nodeOutput.state().value("complete_plan").ifPresent(contentBuilder::append);
                                }
                            }
                        } else {
                            log.info("NodeOutput 不是 StreamingOutput: {}", nodeOutput.getClass());
                        }

                        data.put("content", contentBuilder.toString());
                        data.put("hasContent", hasContent);

                        // 根据 agent 类型设置阶段标识
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
                        log.error("流式创作过程中发生错误", error);

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
                        log.info("流式创作完成");
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
     * 根据 agent 名称确定创作阶段
     */
    private String determineStage(String agentName) {
        if (agentName != null) {
            if (agentName.contains("creative")) {
                return "creative"; // 创意策划
            } else if (agentName.contains("budget")) {
                return "budget"; // 预算评估
            } else if (agentName.contains("execution")) {
                return "execution"; // 执行规划
            } else if (agentName.contains("parallel") || agentName.contains("plan") || agentName.contains("complete")) {
                return "final"; // 最终方案
            }
        }
        return "unknown";
    }
}
