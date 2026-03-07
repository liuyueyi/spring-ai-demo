package com.git.hui.springai.app.protocol.dto;

import lombok.Data;

/**
 * Agent 聊天请求对象
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Data
public class AgentChatRequest {
    /**
     * 会话 ID（可选，不传则自动创建）
     */
    private String sessionId;

    /**
     * 用户消息内容
     */
    private String message;

    /**
     * 上下文
     */
    private CopilotContext context;

    /**
     * 元数据
     */
    private Metadata metadata;

    public Metadata getMetadata() {
        if (metadata == null) {
            metadata = new Metadata();
        }
        return metadata;
    }

    @Data
    public static class CopilotContext {
        /**
         * 应用 ID（建议填写）
         */
        private String appId;

        /**
         * 鉴权 Token
         */
        private String token;

        /**
         * 当前页面 URL
         */
        private String url;

        /**
         * 页面标题
         */
        private String title;
    }

    @Data
    public static class Metadata {
        /**
         * 请求 ID
         */
        private String requestId;

        /**
         * 链路 ID
         */
        private String traceId;

        /**
         * 流恢复锚点
         */
        private ResumeFrom resumeFrom;

        @Data
        public static class ResumeFrom {
            /**
             * 上次处理到的 sequence
             */
            private Integer sequence;

            /**
             * 上次处理到的 eventId
             */
            private String eventId;
        }
    }
}
