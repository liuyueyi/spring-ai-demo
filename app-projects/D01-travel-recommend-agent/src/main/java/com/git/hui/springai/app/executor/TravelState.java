package com.git.hui.springai.app.executor;

import com.git.hui.springai.app.agents.FoodRecommendAgent;
import com.git.hui.springai.app.model.SimpleResVo;
import com.git.hui.springai.app.model.TravelResVo;
import com.git.hui.springai.app.serializer.JsonSerializer;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.bsc.langgraph4j.spring.ai.serializer.std.MessageSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/8/12
 */
public class TravelState extends AgentState {
    /**
     * 最开始的输入参数
     */
    public static final String INPUT = "input";
    /**
     * WeatherAgent执行后的返回的天气信息
     */
    public static final String WEATHER = "weather";
    /**
     * TravelRecommendAgent 执行后返回推荐游玩项目
     */
    public static final String TRAVEL = "recommendation";
    /**
     * FoodRecommendAgent 执行后返回推荐美食
     */
    public static final String FOOD = "food";

    /**
     * 最后基于上面Agent的执行内容，生成的返回给用户的小红书风格的博文内容
     */
    public static final String BLOG = "blog";

    /**
     * 提供序列化方式，默认使用ObjectStreamStateSerializer，无法有效支持Java POJO类的序列化
     *
     * @return An instance of `StateSerializer` for serializing and deserializing `State` objects.
     */
    public static StateSerializer<TravelState> serializer() {
        var serializer = new ObjectStreamStateSerializer<>(TravelState::new);
        serializer.mapper().register(Message.class, new MessageSerializer());
        serializer.mapper().register(FoodRecommendAgent.TravelFoodRecommends.class, new JsonSerializer<>(FoodRecommendAgent.TravelFoodRecommends.class));
        serializer.mapper().register(TravelResVo.class, new JsonSerializer<>(TravelResVo.class));
        serializer.mapper().register(SimpleResVo.class, new JsonSerializer<>(SimpleResVo.class));
        return serializer;
    }

    public TravelState(Map<String, Object> initData) {
        super(initData);
    }

    public String getInput() {
        return (String) value(INPUT).orElse("");
    }

    public String getWeather() {
        return (String) value(WEATHER).orElse("");
    }

    public String getTravel() {
        return (String) value(TRAVEL).orElse("");
    }

    public FoodRecommendAgent.TravelFoodRecommends getFood() {
        return (FoodRecommendAgent.TravelFoodRecommends) value(FOOD).orElse(new FoodRecommendAgent.TravelFoodRecommends("", List.of()));
    }

    public SimpleResVo getBlog() {
        return (SimpleResVo) value(BLOG).orElse(new SimpleResVo());
    }
}
