package com.git.hui.springai.app.agents;

import com.git.hui.springai.app.executor.TravelState;
import com.git.hui.springai.app.service.AgentService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/8/12
 */
@Service
public class WeatherAgent {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WeatherAgent.class);

    public static final String NAME = "weatherAgent";

    private final AgentService agentService;

    public WeatherAgent(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Calls the Weather Agent to retrieve weather information based on user input.
     *
     * @param state The current state of the workflow.
     * @return A map containing the weather details as output.
     */
    public Map<String, Object> callWeatherAgent(TravelState state) {
        // 获取用户输入
        var query = state.getInput();
        log.info("[callWeatherAgent]: {}, input: {}", state, query);

        var response = agentService.weather("请帮我查询给定位置的实时天气\n" + query);

        // 将天气的返回结果保存到State中
        Map<String, Object> output = new HashMap<>();
        output.put(TravelState.WEATHER, response);
        log.info("[callWeatherAgent] Output: {}", output);
        return output;
    }
}
