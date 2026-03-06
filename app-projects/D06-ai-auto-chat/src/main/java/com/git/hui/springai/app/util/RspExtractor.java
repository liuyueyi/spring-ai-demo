package com.git.hui.springai.app.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.git.hui.springai.app.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Slf4j
public class RspExtractor {
    public static ObjectMapper objectMapper = new ObjectMapper();

    /**
     *  简化实现，实际应该根据内容智能判断
     */
    public static Object parseStructuredData(String content, String responseType) {
        // 尝试提取 JSON 内容（可能包含在 markdown 代码块中）
        String json = extractJson(content);
        try {
            switch (responseType) {
                case "card":
                    return objectMapper.readValue(json, ChatResponse.CardData.class);
                case "list":
                    return objectMapper.readValue(json, objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, ChatResponse.ListItem.class));
                case "options":
                    return objectMapper.readValue(json, objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, ChatResponse.OptionItem.class));
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warn("Parse structured data failed", e);
            return null;
        }

    }

    /**
     * 从内容中提取 JSON
     */
    private static String extractJson(String content) {
        // 处理 markdown 代码块格式
        if (content.contains("```json")) {
            int start = content.indexOf("```json") + 7;
            int end = content.indexOf("```", start);
            if (end > start) {
                return content.substring(start, end).trim();
            }
        } else if (content.contains("```")) {
            int start = content.indexOf("```") + 3;
            int end = content.indexOf("```", start);
            if (end > start) {
                return content.substring(start, end).trim();
            }
        }

        // 如果没有代码块，尝试直接找到 JSON 的起始和结束位置
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }

        // 如果是数组
        start = content.indexOf('[');
        end = content.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }

        return content.trim();
    }
}
