package com.git.hui.springai.agentscope;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * 天气查询工具 —— Agent 可以调用查询指定日期的气温和天气
 */
public class WeatherTool {
    private static final List<String> WEATHERS = List.of(
            "晴", "晴转多云", "多云", "阴", "小雨", "中雨", "大雨", "阵雨", "雷阵雨", "多云转晴"
    );

    // 新增一个获取今天时间的方法
    @Tool(name = "getToday", description = "获取今天的日期，格式为 yyyy-MM-dd")
    public String getToday() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Tool(name = "getDailyTemperature",
            description = "查询某一天的最高气温和最低气温，入参为日期字符串，格式为 yyyy-MM-dd")
    public String getDailyTemperature(
            @ToolParam(name = "date",
                    description = "查询的日期，格式 yyyy-MM-dd，例如 2026-07-21") String date) {
        System.out.println("[WeatherTool] 查询日期: " + date);

        // 根据日期生成确定性的随机种子，同一天返回相同结果
        try {
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            int dayOfYear = localDate.getDayOfYear();
            int month = localDate.getMonthValue();

            // 用日期做种子，保证同一天结果一致
            Random dateRandom = new Random(dayOfYear * 31L + month);

            // 根据月份调整基准温度（夏季高温、冬季低温）
            int baseHigh = switch (month) {
                case 12, 1, 2 -> 5 + dateRandom.nextInt(10);   // 冬季 5~15°C
                case 3, 4 -> 15 + dateRandom.nextInt(10);       // 春季 15~25°C
                case 5, 6 -> 25 + dateRandom.nextInt(10);       // 初夏 25~35°C
                case 7, 8 -> 30 + dateRandom.nextInt(8);        // 盛夏 30~38°C
                case 9, 10 -> 20 + dateRandom.nextInt(10);      // 秋季 20~30°C
                case 11 -> 10 + dateRandom.nextInt(10);          // 深秋 10~20°C
                default -> 20 + dateRandom.nextInt(10);
            };

            int lowOffset = 5 + dateRandom.nextInt(8);          // 温差 5~12°C
            int low = baseHigh - lowOffset;

            String weather = WEATHERS.get(dateRandom.nextInt(WEATHERS.size()));

            return String.format("%s 最高温 %d°C，最低温 %d°C，%s",
                    date, baseHigh, low, weather);
        } catch (Exception e) {
            return date + " 日期格式错误，请使用 yyyy-MM-dd 格式";
        }
    }
}