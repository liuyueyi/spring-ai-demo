package com.git.hui.springai.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.git.hui.springai.app.dto.ChatRequest;
import com.git.hui.springai.app.dto.ChatResponse;
import com.git.hui.springai.app.util.RspExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * AI 聊天服务
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatModel chatModel;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    private final ChatMemory chatMemory;

    public ChatService(ChatModel chatModel, ObjectMapper objectMapper, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 流式聊天，返回 Flux<ChatResponse>
     *
     * @param request 请求对象
     * @return 流式响应
     */
    public Flux<ChatResponse> streamChat(ChatRequest request) {
        String conversationId = getOrCreateConversationId(request.getConversationId());

        // 构建提示词，根据响应类型调整输出格式
        String prompt = buildPrompt(request.getMessage(), request.getResponseType(), conversationId);

        // 使用 ChatClient 进行流式调用
        StringBuilder context = new StringBuilder();

        return chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .map(content -> {
                    context.append(content);
                    return createStreamResponse(content, request.getResponseType(), conversationId);
                })
                .concatWith(Flux.defer(() -> {
                    // 流式完成后，返回完整的结构化数据
                    log.info("Stream completed, context length: {}", context.length());
                    Object structuredData = RspExtractor.parseStructuredData(context.toString(), request.getResponseType());
                    ChatResponse struct = null;
                    if (structuredData != null) {
                        // 返回结构化的数据
                        struct = ChatResponse.builder()
                                .type(request.getResponseType())
                                .done(false)
                                .conversationId(conversationId)
                                .data(structuredData)
                                .build();
                    }
                    ChatResponse finalResponse = ChatResponse.builder()
                            .type(request.getResponseType())
                            .conversationId(conversationId)
                            .done(true)
                            .build();
                    if (struct != null) return Flux.just(struct, finalResponse);
                    return Flux.just(finalResponse);
                }))
                .onErrorResume(error -> {
                    log.error("Stream chat error", error);
                    return Flux.just(ChatResponse.builder()
                            .type(request.getResponseType())
                            .error(error.getMessage())
                            .conversationId(conversationId)
                            .done(true)
                            .build());
                });
    }

    /**
     * 普通聊天，返回完整响应
     *
     * @param request 请求对象
     * @return 完整响应
     */
    public ChatResponse chat(ChatRequest request) {
        String conversationId = getOrCreateConversationId(request.getConversationId());

        // 构建提示词
        String prompt = buildPrompt(request.getMessage(), request.getResponseType(), conversationId);

        try {
            // 使用 ChatClient 进行同步调用
            String content = chatClient.prompt(prompt)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();

            return parseResponse(content, request.getResponseType(), conversationId);
        } catch (Exception e) {
            log.error("Chat error", e);
            return ChatResponse.builder()
                    .type(request.getResponseType())
                    .error(e.getMessage())
                    .conversationId(conversationId)
                    .done(true)
                    .build();
        }
    }

    /**
     * 获取或创建会话 ID
     */
    private String getOrCreateConversationId(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return conversationId;
    }

    /**
     * 根据响应类型构建提示词
     */
    private String buildPrompt(String message, String responseType, String conversationId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(message);

        switch (responseType) {
            case "card":
                prompt.append("\n\n请以 JSON 格式返回一个卡片结构，包含 title(标题), subtitle(副标题), description(描述) 字段");
                break;
            case "list":
                prompt.append("\n\n请以 JSON 数组格式返回列表，每个列表项包含 title(标题), description(描述) 字段");
                break;
            case "options":
                prompt.append("\n\n请以 JSON 数组格式返回选项列表，每个选项包含 label(标签), value(值), description(描述) 字段");
                break;
            default:
                // text 类型不需要特殊处理
                prompt.append("\n\n请直接返回文本");
                break;
        }
        return prompt.toString();
    }

    /**
     * 创建流式响应（只返回文本片段，不解析结构化数据）
     */
    private ChatResponse createStreamResponse(String content, String responseType, String conversationId) {
        // 流式返回时，直接返回文本片段
        return ChatResponse.builder()
                .type(responseType)
                .content(content)
                .conversationId(conversationId)
                .done(false)
                .build();
    }

    /**
     * 解析响应内容（用于非流式完整响应）
     */
    private ChatResponse parseResponse(String content, String responseType, String conversationId) {
        ChatResponse.ChatResponseBuilder builder = ChatResponse.builder()
                .type(responseType)
                .conversationId(conversationId);

        try {
            if ("text".equals(responseType)) {
                // 纯文本直接返回
                return builder.content(content).done(true).build();
            } else {
                // 尝试解析结构化数据
                Object data = RspExtractor.parseStructuredData(content, responseType);
                if (data != null) {
                    // 解析成功，返回结构化数据和空文本
                    return builder.data(data).content("").done(true).build();
                } else {
                    // 解析失败，作为文本返回
                    return builder.content(content).done(true).build();
                }
            }
        } catch (Exception e) {
            log.warn("Parse response failed, return as text", e);
            return builder.content(content).done(true).build();
        }
    }

}
