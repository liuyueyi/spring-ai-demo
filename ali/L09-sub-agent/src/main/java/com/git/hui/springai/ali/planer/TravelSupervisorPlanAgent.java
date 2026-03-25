package com.git.hui.springai.ali.planer;

import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.git.hui.springai.ali.context.AgentTool;
import com.git.hui.springai.ali.hook.HookFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 *
 * @author YiHui
 * @date 2026/3/22
 */
@RequiredArgsConstructor
@Component
public class TravelSupervisorPlanAgent {
    private final AttractionAgent attractionAgent;
    private final HotelAgent hotelAgent;
    private final TransportAgent transportAgent;
    private final ChatModel chatModel;

    public Agent agent(String sessionId) {
        ReactAgent agent = ReactAgent.builder()
                .name("supervisor_agent")
                .model(chatModel)
                .systemPrompt("""
                        你是一个旅行规划监督者，你的任务是帮助用户制定旅行计划。
                        你可以协调以下专家：
                        - attraction_agent：负责推荐景点、安排游览路线。
                        - transport_agent：负责规划城市间交通（如飞机、火车）和市内交通。
                        - hotel_agent：负责推荐酒店、民宿等住宿。

                        请记得在制定计划时，始终现有调用 attraction_agent 来推荐景点、安排旅游路线；
                        然后在根据推荐的景点来调用 transport_agent 和 hotel_agent，用于规划行程和住宿情况
                        最后需要将所有的专家输出的内容进行重新组织整合，输出完整的方案给用户
                          """)
                .saver(new MemorySaver())
                // 为每个子 Agent 提供清晰、准确的 description 非常重要，这直接影响主 Agent 如何选择合适的工具。描述应该简洁地说明 Agent 的职责和能力。
                // 使用 StreamingAgentTool 实现流式的工具调用
                .tools(AgentTool.create(attractionAgent.agent())
                        , AgentTool.create(hotelAgent.agent())
                        , AgentTool.create(transportAgent.agent())
                )
                .hooks(HookFactory.createLogModelHook())
                .build();
        return agent;
    }

}
