package com.git.hui.springai.app.agents;

import com.git.hui.springai.app.executor.TravelState;
import com.git.hui.springai.app.service.AgentService;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/8/12
 */
@Service
public class TravelRecommendAgent {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TravelRecommendAgent.class);
    public static final String NAME = "travelAgent";
    private final AgentService agentService;

    public TravelRecommendAgent(AgentService agentService) {
        this.agentService = agentService;
    }

    public Map<String, Object> callTravelAgent(TravelState state) {
        var area = state.getInput();
        var weather = state.getWeather();

        log.info("[callTravelAgent]: {}, weather: {}", state, weather);

        String prompts = """
                请结合现在的天气，帮我推荐三个适合这个天气游玩的项目
                {area}
                {weather}
                """;
        PromptTemplate promptTemplate = new PromptTemplate(prompts);
        String prompt = promptTemplate.render(Map.of("area", area, "weather", weather));
        var recommendation = agentService.travel(prompt);

        Map<String, Object> output = new HashMap<>();
        output.put(TravelState.TRAVEL, recommendation);
        log.info("[callTravelAgent] Output: {}", output);
        return output;
    }
}
