package com.git.hui.springai.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 聊天响应对象
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
    /**
     * 响应类型：text-纯文本，card-卡片，list-列表，options-选项
     */
    private String type;
    
    /**
     * 文本内容（当 type=text 时）
     */
    private String content;
    
    /**
     * 结构化数据（当 type=card/list/options 时）
     */
    private Object data;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 会话 ID
     */
    private String conversationId;
    
    /**
     * 是否流式结束标记
     */
    private Boolean done;
    
    /**
     * 卡片数据结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardData {
        /**
         * 卡片标题
         */
        private String title;
        
        /**
         * 卡片副标题
         */
        private String subtitle;
        
        /**
         * 卡片内容
         */
        private String description;
        
        /**
         * 图片 URL（可选）
         */
        private String imageUrl;
        
        /**
         * 操作按钮
         */
        private List<Action> actions;
        
        /**
         * 额外信息
         */
        private Map<String, Object> extra;
    }
    
    /**
     * 列表项数据结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        /**
         * 列表项标题
         */
        private String title;
        
        /**
         * 列表项描述
         */
        private String description;
        
        /**
         * 列表项图标或图片 URL
         */
        private String iconUrl;
        
        /**
         * 额外信息
         */
        private Map<String, Object> extra;
    }
    
    /**
     * 选项数据结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionItem {
        /**
         * 选项标签
         */
        private String label;
        
        /**
         * 选项值
         */
        private String value;
        
        /**
         * 选项描述
         */
        private String description;
    }
    
    /**
     * 操作按钮
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Action {
        /**
         * 按钮文本
         */
        private String text;
        
        /**
         * 按钮动作值
         */
        private String actionValue;
        
        /**
         * 按钮类型：primary-主要，secondary-次要
         */
        private String actionType;
    }
}
