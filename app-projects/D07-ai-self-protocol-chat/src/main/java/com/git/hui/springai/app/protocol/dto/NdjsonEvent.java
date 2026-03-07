package com.git.hui.springai.app.protocol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * NDJSON 事件对象
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NdjsonEvent {
    /**
     * 事件类型
     */
    private String type;
    
    /**
     * 事件载荷
     */
    private Map<String, Object> payload;
    
    /**
     * 元信息
     */
    private EventMeta meta;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventMeta {
        /**
         * 链路追踪 ID
         */
        private String traceId;
        
        /**
         * 请求 ID
         */
        private String requestId;
        
        /**
         * 会话 ID
         */
        private String sessionId;
        
        /**
         * 序列号
         */
        private Integer sequence;
        
        /**
         * 事件 ID
         */
        private String eventId;
        
        /**
         * 时间戳
         */
        private String ts;
        
        /**
         * 协议版本
         */
        private String protocolVersion;
    }
}
