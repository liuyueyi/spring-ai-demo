package com.git.hui.springai.app.protocol.util;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Slf4j
public class RspExtractor {
    /**
     * 从内容中提取 JSON
     */
    public static String extractJson(String content) {
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
