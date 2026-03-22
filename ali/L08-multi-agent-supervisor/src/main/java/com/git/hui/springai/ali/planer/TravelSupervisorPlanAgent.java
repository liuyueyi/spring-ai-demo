package com.git.hui.springai.ali.planer;

import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.git.hui.springai.ali.controller.HookFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public Agent agent() {
//        ReactAgent agent = ReactAgent.builder()
//                .name("supervisor_agent")
//                .model(chatModel)
//                .systemPrompt("""
//                        你是一个旅行规划监督者，你的任务是帮助用户制定旅行计划。
//                        你可以协调以下专家：
//                        - attraction_agent：负责推荐景点、安排游览路线。
//                        - transport_agent：负责规划城市间交通（如飞机、火车）和市内交通。
//                        - hotel_agent：负责推荐酒店、民宿等住宿。
//
//                        请记得在制定计划时，始终现有调用 attraction_agent 来推荐景点、安排旅游路线；因为 transport_agent 和 hotel_agent 这两个子Agent的执行都依赖 attraction_agent 的输出
//                        你需要根据用户的需求，逐步安排专家工作。最后需要将所有的专家输出的内容进行重新组织整合，输出完整的方案给用户
//                          """)
//                .saver(new MemorySaver())
//                .tools(AgentTool.getFunctionToolCallback(attractionAgent.agent())
//                        , AgentTool.getFunctionToolCallback(hotelAgent.agent())
//                        , AgentTool.getFunctionToolCallback(transportAgent.agent())
//                )
//                .build();
//        return agent;

        AgentHook logHook = HookFactory.createLogAgentHook();
        SupervisorAgent supervisorAgent = SupervisorAgent.builder()
                .name("supervisor_agent")
                .description("内容管理监督者，负责协调景点推荐、城市规划、住宿推荐等")
                .model(chatModel)
                .mainAgent(ReactAgent.builder()
                        .name("main_agent")
                        .model(chatModel)
                        .description("监督者主Agent，负责路由决策")
                        .systemPrompt("""
                                你是一个旅行规划监督者，你的任务是帮助用户制定旅行计划。
                                你可以协调以下专家：
                                - attraction_agent：负责推荐景点、安排游览路线。
                                - transport_agent：负责规划城市间交通（如飞机、火车）和市内交通。
                                - hotel_agent：负责推荐酒店、民宿等住宿。

                                ## 路由决策输出格式（仅在选择子Agent时适用）
                                当且仅当需要做出路由决策（选择下一个要调用的子Agent或结束任务）时，请以 JSON 数组格式输出，供系统解析路由；此格式仅用于本次路由，不影响你在其他场景下的主要任务输出格式。
                                - 选择单个子Agent 时输出: ["attraction_agent"] 或 ["transport_agent"] 或 ["hotel_agent"]
                                - 选择多个子Agent 时输出：["transport_agent, "hotel_agent"]
                                - 任务全部完成时输出: [] 或 ["FINISH"]
                                合法元素仅限: attraction_agent、attraction_agent、hotel_agent、FINISH。
                                做路由决策时只输出上述 JSON 数组，不要包含其他解释。
                                """)
                        .instruction("用户的请求是: {input}")
                        .outputKey("final_output")
                        .build())
                .subAgents(List.of(attractionAgent.agent(), hotelAgent.agent(), transportAgent.agent()))
                .hooks(List.of(logHook))
                .build();

        return supervisorAgent;
    }
}
