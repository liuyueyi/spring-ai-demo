package com.git.hui.springai.app.mvc;

import com.git.hui.springai.app.dto.ChatRequest;
import com.git.hui.springai.app.dto.ChatResponse;
import com.git.hui.springai.app.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Agent 聊天控制器（简单版本）
 *
 * @author YiHui
 * @date 2026/3/5
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class SimpleChatController {
    private static final Logger log = LoggerFactory.getLogger(SimpleChatController.class);

    private final ChatService chatService;

    public SimpleChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 流式聊天接口（SSE 方式）
     * POST /api/chat/stream
     * Content-Type: application/json
     * Accept: text/event-stream
     *
     * Body: {
     *   "message": "用户消息",
     *   "conversationId": "会话 ID（可选）",
     *   "responseType": "text|card|list|options"
     * }
     *
     * Response: SSE 流，每行一个 JSON
     * {"type":"text","content":"部","conversationId":"xxx","done":false}
     * {"type":"text","content":"分","conversationId":"xxx","done":false}
     * {"type":"text","content":"","conversationId":"xxx","done":true}
     *
     * @param request 请求对象
     * @return 流式响应
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamChat(@RequestBody ChatRequest request) {
        log.info("Stream chat request: {}", request);
        return chatService.streamChat(request);
    }

    /**
     * 普通聊天接口（非流式）
     * POST /api/chat
     * Content-Type: application/json
     *
     * Body: {
     *   "message": "用户消息",
     *   "conversationId": "会话 ID（可选）",
     *   "responseType": "text|card|list|options"
     * }
     *
     * Response: {
     *   "type": "text",
     *   "content": "AI 回复的内容",
     *   "conversationId": "xxx",
     *   "done": true
     * }
     *
     * @param request 请求对象
     * @return 完整响应
     */
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        log.info("Chat request: {}", request);
        return chatService.chat(request);
    }

    /**
     * GET 方式的流式聊天接口（方便测试）
     * GET /api/chat/stream?message=xxx&conversationId=xxx&responseType=text
     *
     * @param message 消息内容
     * @param conversationId 会话 ID
     * @param responseType 响应类型
     * @return 流式响应
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamChatGet(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId,
            @RequestParam(defaultValue = "text") String responseType) {

        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setConversationId(conversationId);
        request.setResponseType(responseType);

        log.info("Stream chat GET request: {}", request);
        return chatService.streamChat(request);
    }
}
