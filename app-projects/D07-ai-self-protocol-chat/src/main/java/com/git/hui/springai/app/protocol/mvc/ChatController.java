package com.git.hui.springai.app.protocol.mvc;

import com.git.hui.springai.app.protocol.dto.AgentChatRequest;
import com.git.hui.springai.app.protocol.dto.NdjsonEvent;
import com.git.hui.springai.app.protocol.service.AgentChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Agent 聊天控制器（对齐 Copilot 协议）
 *
 * @author YiHui
 * @date 2026/3/5
 */
@RestController
@RequestMapping("/agent")
@CrossOrigin(origins = "*")
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final AgentChatService agentChatService;

    public ChatController(AgentChatService agentChatService) {
        this.agentChatService = agentChatService;
    }

    /**
     * Agent 流式聊天接口（对齐 Copilot 协议）
     * POST /agent/chat
     * Content-Type: application/json
     * Accept: application/x-ndjson
     *
     * Request Body: AgentChatRequest
     * Response: NDJSON 流，每行一个 NdjsonEvent
     *
     * @param request 请求对象
     * @return 流式响应
     */
    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<NdjsonEvent> chat(@RequestBody AgentChatRequest request) {
        log.info("Agent chat request: sessionId={}, message={}",
                request.getSessionId(), request.getMessage());
        return agentChatService.streamChat(request);
    }

    // 结束会话
    @PostMapping(value = "/session/{id}/end", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<NdjsonEvent> endSession(@PathVariable String id) {
        log.info("End session: {}", id);
        return agentChatService.endSession(id);
    }

    /**
     * 获取会话详情
     * GET /agent/session/{id}
     *
     * @param id 会话 ID
     * @return 会话信息
     */
    @GetMapping("/session/{id}")
    public Map<String, Object> getSession(@PathVariable String id) {
        log.info("Get session: {}", id);
        return agentChatService.getSession(id);
    }

    /**
     * 获取会话列表
     * GET /agent/sessions
     *
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public Map<String, Object> getSessions() {
        log.info("Get all sessions");
        return agentChatService.getAllSessions();
    }
}
