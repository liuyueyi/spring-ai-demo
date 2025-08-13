package com.git.hui.springai.app.agents;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.git.hui.springai.app.executor.TravelState;
import com.git.hui.springai.app.service.AgentService;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/8/12
 */
@Service
public class FoodRecommendAgent {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FoodRecommendAgent.class);
    public static final String NAME = "foodAgent";
    private final AgentService agentService;

    public FoodRecommendAgent(AgentService agentService) {
        this.agentService = agentService;
    }

    public Map<String, Object> callFoodAgent(TravelState state) {
        var travel = state.getTravel();
        log.info("[callFoodAgent]: {}, travel: {}", state, travel);

        // 格式化输出
        var outputConverter = new BeanOutputConverter<>(TravelFoodRecommends.class);

        // 提示词模板
        var prompt = """
                现在的天气是 {weather}
                请结合下面推荐的旅游游玩项目，帮我为每个项目推荐三种美食
                {travel}
                {format}
                """;
        var promptTemplate = new PromptTemplate(prompt);
        var userMsg = promptTemplate.render(Map.of("weather", state.getWeather(), "travel", travel, "format", outputConverter.getFormat()));

        var foodSuggestion = agentService.food(userMsg);
        var res = outputConverter.convert(foodSuggestion);

        Map<String, Object> output = new HashMap<>();
        output.put(TravelState.FOOD, res);
        log.info("Food Agent Output: {}", output);
        return output;
    }

    public record TravelFoodRecommends(@JsonPropertyDescription("天气") String weather,
                                       @JsonPropertyDescription("不同旅游项目的美食推荐") List<FoodSuggestion> food) {
    }

    public record FoodSuggestion(@JsonPropertyDescription("推荐的游玩项目") String travel,
                                 @JsonPropertyDescription("项目推荐原因") String travelReason,
                                 @JsonPropertyDescription("适合这个旅游项目的美食推荐列表") List<FoodItem> foods) {
    }

    public record FoodItem(@JsonPropertyDescription("推荐的美食名") String foodName,
                           @JsonPropertyDescription("推荐这个美食的原因") String recommendReason) {

    }
}
