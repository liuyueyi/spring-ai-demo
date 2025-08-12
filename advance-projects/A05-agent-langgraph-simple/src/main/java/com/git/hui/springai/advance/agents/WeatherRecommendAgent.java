package com.git.hui.springai.advance.agents;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.utils.EdgeMappings;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

// 内部类：构建并执行 StateGraph 的服务
public class WeatherRecommendAgent {
    private final ChatClient chatClient;
    private final CompiledGraph<AgentState> graph;

    public WeatherRecommendAgent(ChatClient chatClient) throws GraphStateException {
        this.chatClient = chatClient;
        this.graph = initGraph("北京");

        this.printPlantUml();
    }

    private CompiledGraph<AgentState> initGraph(String location) throws GraphStateException {
        // Node1: weather agent - 这里示例使用简单规则模拟天气（生产可以换成真实天气 API）
        NodeAction<AgentState> weatherNode = state -> {
            // 取入参 location（状态里可能已有）
            String loc = (String) state.value("location").orElseGet(() -> location);
            // 简单随机/固定返回以示范。生产请替换为天气 API 的结果（"晴天"/"雨天"/"阴天" 等）
            // 这里为了 demo，按 location 最后一个字判断（仅示例）
            String weather;
            if (loc.endsWith("市") || loc.endsWith("区")) weather = "晴天";
            else if (loc.endsWith("省")) weather = "阴天";
            else weather = "雨天";

            System.out.println("[weatherNode] location=" + loc + " => weather=" + weather);
            return Map.of(
                    "location", loc,
                    "weather", weather
            );
        };

        // Node2: router - 只是做路由，本节点不做 State 的任何更新
        NodeAction<AgentState> routerNode = state -> {
            // 这个节点，用于模拟啥也不干的场景
            String w = (String) state.value("weather").get();
            System.out.println("[routerNode] weather=" + w);
            return Map.of(); // 不改变状态
        };

        // Node3: outdoor - 用大模型生成外出推荐
        NodeAction<AgentState> outdoorNode = state -> {
            String loc = (String) state.value("location").orElseGet(() -> location);
            String weather = (String) state.value("weather").orElseGet(() -> "晴天");

            String prompt = String.format(
                    "你是一个资深旅行推荐师：用户在地点“%s”，当前天气“%s”。请用中文给出 3 个适合外出（户外）游玩的项目，每个项目写一行：项目名称 - 30 字以内简短描述 - 预计耗时。不要写多余开头语，返回纯文本列表。",
                    loc, weather);

            String rec = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            System.out.println("[outdoorNode] model result:\n" + rec);
            return Map.of("outdoor_recommendations", rec);
        };

        // Node4: indoor - 用大模型生成室内推荐
        NodeAction<AgentState> indoorNode = state -> {
            String loc = (String) state.value("location").orElseGet(() -> location);
            String weather = (String) state.value("weather").orElseGet(() -> "雨天");

            String prompt = String.format(
                    "你是一个资深旅行推荐师：用户在地点“%s”，当前天气“%s”。请用中文给出 3 个适合室内游玩的项目，每个项目写一行：项目名称 - 30 字以内简短描述 - 预计耗时。不要写多余开头语，返回纯文本列表。",
                    loc, weather);

            String rec = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            System.out.println("[indoorNode] model result:\n" + rec);
            return Map.of("indoor_recommendations", rec);
        };

        // Build StateGraph
        var graph = new StateGraph<>(AgentState::new)
                .addNode("weather", AsyncNodeAction.node_async(weatherNode))
                .addNode("router", AsyncNodeAction.node_async(routerNode))
                .addNode("outdoor", AsyncNodeAction.node_async(outdoorNode))
                .addNode("indoor", AsyncNodeAction.node_async(indoorNode))

                // entry
                .addEdge(START, "weather")
                // weather -> router
                .addEdge("weather", "router")
                // router 根据 state 决定去哪里
                .addConditionalEdges("router", new RouteEvaluationResult(), EdgeMappings.builder()
                        .to("outdoor", "outdoor")
                        .to("indoor", "indoor")
                        .toEND()
                        .build())
                // 输出结束
                .addEdge("outdoor", END)
                .addEdge("indoor", END)
                .compile();
        return graph;
    }

    public static class RouteEvaluationResult implements AsyncEdgeAction<AgentState> {
        @Override
        public CompletableFuture<String> apply(AgentState agentState) {
            String w = (String) agentState.value("weather").orElseGet(() -> "晴天");
            String res;
            if ("晴天".equalsIgnoreCase(w)) {
                res = "outdoor";
            } else if ("雨天".equalsIgnoreCase(w)) {
                res = "indoor";
            } else {
                // 其余天气直接结束
                res = END;
            }
            return CompletableFuture.completedFuture(res);
        }
    }

    /**
     * 打印 plantUml 格式流程图
     *
     * @return
     */
    public String printPlantUml() {
        GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML, "旅游推荐Agent", true);
        // 获取 PlantUML 文本
        System.out.println("=== PlantUML 图 ===");
        System.out.println(representation.content());
        System.out.println("------- UML图结束 ---------");
        return representation.content();
    }

    /**
     * 通过给定的地方，返回旅游推荐项目
     *
     * @param location 地区
     * @return
     */
    public Map<String, Object> recommendByLocation(String location) {
        // 初始 state，用于上下文传参
        Map<String, Object> init = new HashMap<>();
        init.put("location", location);

        // 执行图
        AgentState last = null;
        for (var item : graph.stream(init)) {
            // 打印过程记录
            System.out.println(item);
            last = item.state();
        }
        // 返回最后的结果
        return last.data();
    }
}