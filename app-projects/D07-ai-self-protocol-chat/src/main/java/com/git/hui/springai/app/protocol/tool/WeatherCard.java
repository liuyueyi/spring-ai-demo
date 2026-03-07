package com.git.hui.springai.app.protocol.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 天气卡片数据
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherCard {
    /**
     * 城市名称
     */
    private String city;

    /**
     * 天气状况（晴、多云、雨等）
     */
    private String condition;

    /**
     * 温度（摄氏度）
     */
    private Integer temperature;

    /**
     * 湿度（百分比）
     */
    private Integer humidity;

    /**
     * 空气质量指数
     */
    private Integer aqi;

    /**
     * 风向
     */
    private String windDirection;

    /**
     * 风力等级
     */
    private String windLevel;

    /**
     * 建议穿着
     */
    private String dressAdvice;

    /**
     * 温馨提示
     */
    private String tips;
}
