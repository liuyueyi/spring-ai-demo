package com.git.hui.springai.app.protocol.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 柱状图卡片数据
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarChartCard {
    /**
     * 图表标题
     */
    private String title;

    /**
     * 图表描述（可选）
     */
    private String description;

    /**
     * X 轴标签
     */
    private List<String> xAxis;

    /**
     * 数据集列表
     */
    private List<Dataset> datasets;

    /**
     * Y 轴单位
     */
    private String yAxisLabel;

    /**
     * 是否显示数值标签
     */
    private Boolean showLabels = true;

    /**
     * 颜色方案
     */
    private ColorScheme colorScheme;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dataset {
        /**
         * 数据集名称
         */
        private String label;

        /**
         * 数据值列表
         */
        private List<Integer> data;

        /**
         * 颜色（16 进制）
         */
        private String backgroundColor;

        /**
         * 边框颜色
         */
        private String borderColor;
    }

    public enum ColorScheme {
        DEFAULT,    // 默认
        BLUE,       // 蓝色系
        GREEN,      // 绿色系
        RED,        // 红色系
        RAINBOW     // 彩虹色
    }
}
