package com.git.hui.springai.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识问答题卡片数据
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizCard {
    /**
     * 问题
     */
    private String question;

    /**
     * 问题描述（可选）
     */
    private String description;

    /**
     * 候选项列表
     */
    private List<Option> options;

    /**
     * 正确答案（用于验证，不返回给前端）
     */
    private String correctAnswer;

    /**
     * 题目解析
     */
    private String explanation;

    /**
     * 难度等级
     */
    private Difficulty difficulty;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        /**
         * 选项标识（A, B, C, D）
         */
        private String key;

        /**
         * 选项内容
         */
        private String value;

        /**
         * 选项描述（可选）
         */
        private String description;
    }

    public enum Difficulty {
        EASY,       // 简单
        MEDIUM,     // 中等
        HARD,       // 困难
        EXPERT      // 专家
    }
}
