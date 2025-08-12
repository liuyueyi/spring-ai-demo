package com.git.hui.springai.advance.times;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 根据时区，返回时间的工具
 *
 * @author YiHui
 * @date 2025/8/8
 */
public class TimeWeatherTools {
    @Tool(description = "传入时区，返回对应时区的当前时间给用户")
    public String getTimeByZoneId(@ToolParam(description = "需要查询时间的时区，如Asia/Shanghai, Europe/Paris") ZoneId area) {
        // 根据系统当前时间，获取指定时区的时间
        ZonedDateTime time = ZonedDateTime.now(area);

        // 格式化时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String ans = time.format(formatter);
        System.out.println("传入的时区是：" + area + "-" + ans);
        return ans;
    }

    @Tool(description = "传入地点，返回对应地点的当前天气给用户")
    public String getWeatherByZoneId(@ToolParam(description = "需要查询天气的地区，如北京、上海") String area) {
        List<String> weathers = List.of("晴", "阴", "雨", "雪", "雷", "雾");
        String ans = weathers.get((int) (Math.random() * weathers.size()));
        System.out.println("传入的地点是：" + area + "-" + ans);
        return ans;
    }
}
