package com.git.hui.springai.ali.mvc;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.git.hui.springai.ali.express.HandoffExpressOrderHook;
import com.git.hui.springai.ali.express.tools.ExpressOrderTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 快递下单聊天控制器
 * 提供基于对话的快递下单功能
 * 
 * @author YiHui
 * @date 2026/3/12
 */
@Slf4j
@RestController
@RequestMapping("/express")
public class ExpressChatController {

    private final ChatModel chatModel;
    private final MemorySaver memorySaver;
    
    // 存储每个会话的 Agent 实例
    private final Map<String, ReactAgent> agentMap = new ConcurrentHashMap<>();

    public ExpressChatController(ChatModel chatModel, MemorySaver memorySaver) {
        this.chatModel = chatModel;
        this.memorySaver = memorySaver;
    }

    /**
     * 获取或创建会话的 Agent
     */
    private ReactAgent getOrCreateAgent(String sessionId) {
        return agentMap.computeIfAbsent(sessionId, key -> {
            HandoffExpressOrderHook expressHook = new HandoffExpressOrderHook();

            ToolCallback receiveAddress = ExpressOrderTools.findByName("receiveAddress");
            ToolCallback sendAddress = ExpressOrderTools.findByName("sendAddress");
            ToolCallback expressInfo = ExpressOrderTools.findByName("expressInfo");
            ToolCallback showExpressOrder = ExpressOrderTools.findByName("showExpressOrder");
            ToolCallback createOrder = ExpressOrderTools.findByName("createOrder");

            return ReactAgent.builder()
                    .name("express_order_agent")
                    .model(chatModel)
                    .tools(List.of(receiveAddress, sendAddress, expressInfo, showExpressOrder, createOrder))
                    .saver(memorySaver)
                    .hooks(expressHook)
                    .enableLogging(true)
                    .build();
        });
    }

    /**
     * 同步聊天接口
     */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.get("sessionId");
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }
        
        try {
            ReactAgent agent = getOrCreateAgent(sessionId);
            RunnableConfig config = RunnableConfig.builder().threadId(sessionId).build();
            
            AssistantMessage response = agent.call(new UserMessage(message), config);
            
            return Map.of(
                "sessionId", sessionId,
                "response", response.getText()
            );
        } catch (Exception e) {
            log.error("聊天处理失败", e);
            return Map.of(
                "sessionId", sessionId,
                "response", "抱歉，处理您的请求时出现错误：" + e.getMessage()
            );
        }
    }

    /**
     * 流式聊天接口（SSE）
     */
    @PostMapping(value = "/chatStream", produces = "text/event-stream")
    public Flux<Map<String, String>> chatStream(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());
        
        try {
            ReactAgent agent = getOrCreateAgent(sessionId);
            RunnableConfig config = RunnableConfig.builder().threadId(sessionId).build();
            
            // 使用流式调用
            return Flux.just(agent.call(new UserMessage(message), config))
                    .map(response -> Map.of(
                        "sessionId", sessionId,
                        "response", response.getText()
                    ));
        } catch (Exception e) {
            log.error("流式聊天处理失败", e);
            return Flux.just(Map.of(
                "sessionId", sessionId,
                "response", "抱歉，处理您的请求时出现错误：" + e.getMessage()
            ));
        }
    }

    /**
     * 重置会话
     */
    @PostMapping("/reset")
    public Map<String, String> reset(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        if (sessionId != null) {
            agentMap.remove(sessionId);
        }
        String newSessionId = UUID.randomUUID().toString();
        return Map.of(
            "sessionId", newSessionId,
            "message", "会话已重置，可以开始新的快递下单流程"
        );
    }
}
