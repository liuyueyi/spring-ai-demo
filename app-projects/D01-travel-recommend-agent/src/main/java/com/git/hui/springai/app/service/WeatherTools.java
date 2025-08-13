package com.git.hui.springai.app.service;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

public class WeatherTools {
    private static final Logger log = LoggerFactory.getLogger(WeatherTools.class);

    @Tool(description = "根据传入的地区，返回对应地区的当前天气")
    public WeatherResponse currentWeatherFunction(WeatherRequest request) {
        List<String> weathers = List.of("rain", "cloudy", "storm", "thunder", "snow");
        List<String> temps = List.of("32°", "18°", "10°", "5°", "0°");
        WeatherResponse response = new WeatherResponse(request.area(), weathers.get((int) (Math.random() * weathers.size())) + " " + temps.get((int) (Math.random() * temps.size())));
        log.info("[WeatherTools] response weather： {}", response);
        return response;
    }

    public record WeatherRequest(
            @JsonPropertyDescription("请求的地区，可以是省市区镇街道，如北京、上海、武汉市洪山区、北京朝阳街道") String area) {
    }

    public record WeatherResponse(@JsonPropertyDescription("用户请求的地区") String area,
                                  @JsonPropertyDescription("地区对应的当前天气，如 晴 32°， 小雨 18°") String weather) {
    }
}

