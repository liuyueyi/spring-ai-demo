package com.git.hui.springai.app.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * @author YiHui
 * @date 2025/8/15
 */
public record CardInfo(
        @JsonPropertyDescription("中文词汇，如 猫")
        String zh,
        @JsonPropertyDescription("中文词汇描述，如 猫是一种常见的家养宠物")
        String zhDesc,
        @JsonPropertyDescription("英文单词，如 cat")
        String en,
        @JsonPropertyDescription("英文单词描述，如 A young domestic cat")
        String enDesc) {
}
