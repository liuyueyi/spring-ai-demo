package com.git.hui.springai.app.agents;

import com.git.hui.springai.app.executor.TravelState;
import com.git.hui.springai.app.model.SimpleResVo;
import com.git.hui.springai.app.model.TravelResVo;
import com.git.hui.springai.app.serializer.JsonUtil;
import com.git.hui.springai.app.service.AgentService;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/8/13
 */
@Service
public class XhsBlogGenerateAgent {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(XhsBlogGenerateAgent.class);
    public static final String NAME = "xhsAgent";
    private final AgentService agentService;

    public XhsBlogGenerateAgent(AgentService agentService) {
        this.agentService = agentService;
    }

    public Map<String, Object> callResponseAgent(TravelState state) {
        log.info("[XhsBlogGenerateAgent]: {}", state);

        var response = state.getFood();
        String prompt;
        String systemPrompt;
        BeanOutputConverter<?> outputConverter;
        if (response != null && !CollectionUtils.isEmpty(response.food())) {
            // 从 FoodRecommendAgent 过来的
            outputConverter = new BeanOutputConverter<>(TravelResVo.class);
            systemPrompt = "你现在是一个资深的小红书旅游博主运营专家，擅长根据给出地点、天气、推荐项目、推荐美食来生成吸引人阅读的博文";

            // 有推荐旅游项目和美食的返回
            String prompts = """
                    下面是我现在准备的一些素材，请帮我写一份小红书风格的推荐博文，以中文方式返回
                    {weather}
                    {recommends}
                    {format}
                    """;
            PromptTemplate promptTemplate = new PromptTemplate(prompts);
            prompt = promptTemplate.render(Map.of("weather", state.getWeather(), "recommends", JsonUtil.toStr(response),
                    "format", outputConverter.getFormat()));
        } else {
            // 直接从 WeatherAgent 过来的
            outputConverter = new BeanOutputConverter<>(SimpleResVo.class);
            systemPrompt = "你现在是一个资深的天气预报专家，擅长根据给天气给出合理的关注事项建议";

            // 只返回天气
            String prompts = """
                    下面我输入的是天气，请以贴心关怀的语气生成一段天气预报的文案，并配上这个天气的注意事项，以中文的方式返回给我
                    {weather}
                    {format}
                    """;
            PromptTemplate promptTemplate = new PromptTemplate(prompts);
            prompt = promptTemplate.render(Map.of("weather", state.getWeather(), "format", outputConverter.getFormat()));
        }

        String recommendation = agentService.execute(systemPrompt, prompt);

        Map<String, Object> output = new HashMap<>();
        output.put(TravelState.BLOG, outputConverter.convert(recommendation));
        log.info("[XhsBlogGenerateAgent] Output: {}", output);
        return output;
    }
}
