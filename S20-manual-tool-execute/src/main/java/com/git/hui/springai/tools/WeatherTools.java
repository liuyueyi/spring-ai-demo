package com.git.hui.springai.tools;

import com.git.hui.springai.tools.dto.WeatherCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 *
 * @author YiHui
 * @date 2026/3/6
 */
@Slf4j
@Service
public class WeatherTools {
    /**
     * 查询天气
     *
     * @param city 城市名称
     * @param toolContext 工具上下文（可选）
     * @return 天气卡片数据
     */
    @Tool(name = "queryWeather", description = "查询指定城市的天气信息，返回详细的天气状况、温度、湿度等数据")
    @ToolResponseType("card")  // 声明返回类型为 card
    public WeatherCard queryWeather(
            @ToolParam(description = "城市名称，如北京、上海、广州等") String city,
            ToolContext toolContext) {  // ✅ 添加工具上下文参数

        // 从上下文中获取额外信息
        if (toolContext != null && !toolContext.getContext().isEmpty()) {
            log.info("【工具上下文】queryWeather - context: {}", toolContext.getContext());
            // 可以从中获取 userId, sessionId, appId 等信息
            String userId = (String) toolContext.getContext().get("userId");
            String sessionId = (String) toolContext.getContext().get("sessionId");
            log.info("用户 {} 在会话 {} 中查询 {} 的天气", userId, sessionId, city);
        }

        log.info("[inner-tool] 查询天气：{}", city);

        // TODO: 实际场景中应该调用天气 API
        // 这里使用模拟数据演示
        Random random = new Random();
        int temperature = 15 + random.nextInt(20); // 15-35 度
        int humidity = 40 + random.nextInt(40);    // 40-80%
        int aqi = 20 + random.nextInt(80);         // 20-100

        String[] conditions = {"晴", "多云", "阴", "小雨", "大雨"};
        String condition = conditions[random.nextInt(conditions.length)];

        String[] directions = {"东风", "南风", "西风", "北风", "东南风", "东北风"};
        String windDirection = directions[random.nextInt(directions.length)];
        String windLevel = (random.nextInt(5) + 1) + "级";

        String dressAdvice = getDressAdvice(temperature, condition);
        String tips = getWeatherTips(condition, aqi);

        return WeatherCard.builder()
                .city(city)
                .condition(condition)
                .temperature(temperature)
                .humidity(humidity)
                .aqi(aqi)
                .windDirection(windDirection)
                .windLevel(windLevel)
                .dressAdvice(dressAdvice)
                .tips(tips)
                .build();
    }


    private String getDressAdvice(int temperature, String condition) {
        if (temperature < 10) {
            return "建议穿厚外套或羽绒服，注意保暖";
        } else if (temperature < 20) {
            return "建议穿长袖衬衫或薄外套";
        } else if (temperature < 28) {
            return "建议穿短袖 T 恤，清凉透气";
        } else {
            return "建议穿清凉夏装，注意防暑降温";
        }
    }

    private String getWeatherTips(String condition, int aqi) {
        StringBuilder tips = new StringBuilder();

        switch (condition) {
            case "晴":
                tips.append("阳光明媚，适合户外活动。");
                break;
            case "多云":
                tips.append("云层较多，紫外线适中。");
                break;
            case "阴":
                tips.append("天气阴沉，请保持好心情。");
                break;
            case "小雨":
                tips.append("有小雨，出门请带伞。");
                break;
            case "大雨":
                tips.append("雨势较大，尽量减少外出。");
                break;
        }

        if (aqi <= 50) {
            tips.append("空气质量优，适合户外运动。");
        } else if (aqi <= 100) {
            tips.append("空气质量良好，可正常活动。");
        } else {
            tips.append("空气质量较差，敏感人群减少外出。");
        }

        return tips.toString();
    }

}
