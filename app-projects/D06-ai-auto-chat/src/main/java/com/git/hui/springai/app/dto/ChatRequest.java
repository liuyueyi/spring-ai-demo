package com.git.hui.springai.app.dto;

import lombok.Data;

/**
 * 聊天请求对象
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Data
public class ChatRequest {
    /**
     * 用户消息
     */
    private String message;
    
    /**
     * 会话 ID，用于多轮对话
     */
    private String conversationId;
    
    /**
     * 期望的响应类型：text-纯文本，card-卡片，list-列表，options-选项
     */
    private String responseType = "text";
}
