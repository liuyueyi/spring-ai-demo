package com.git.hui.springai.ali.planer;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.git.hui.springai.ali.controller.TravelPlanController;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 景点推荐专家
 * @author YiHui
 * @date 2026/3/22
 */
@RequiredArgsConstructor
@Component
public class AttractionAgent {
    private final ChatModel chatModel;

    public ReactAgent agent() {
        ReactAgent agent = ReactAgent.builder()
                .name("attraction_agent")
                .model(chatModel)
                .description("景点推荐专家，善于根据用户的需求推荐各种适合游玩的景点")
                .instruction("""
                        你是一个旅行景点推荐专家。请根据用户的目的地、天数、偏好，推荐合适的景点和游览顺序。
                        当前已知信息：{input}
                        如果某些信息不完整，请基于已有信息尽量给出合理建议。
                        直接返回景点计划，包含每日安排。
                        """)
//                .outputType(AttractionAgent.class)
                .outputKey("attraction_plan")
                .enableLogging(true)
                .build();

        return agent;
    }

    public record AttractionPlan(String attractionPlan) {
    }
}
