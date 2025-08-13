package com.git.hui.springai.app.test;

import com.git.hui.springai.app.service.AgentService;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * @author YiHui
 * @date 2025/8/12
 */
@Service
public class DemoAgentExecutor {
    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private final AgentService agentService;

    /**
     * Constructor to inject dependencies.
     *
     * @param agentService The service used for agent interactions.
     */
    @Autowired
    public DemoAgentExecutor(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Calls the Weather Agent to retrieve weather information based on user input.
     *
     * @param state The current state of the workflow.
     * @return A map containing the weather details as output.
     */
    Map<String, Object> callWeatherAgent(State state) {
        log.info("callWeatherAgent: {}", state);

        log.info("Weather Agent Input: {}", state.getInput());

        var query = (String) state.getInput().get("query");
        var response = agentService.weather(query);

        Map<String, Object> output = new HashMap<>();
        output.put("weather", response);
        log.info("Weather Agent Output: {}", output);

        return Map.of(State.WEATHER, output);
    }

    /**
     * Calls the Travel Agent to provide travel recommendations based on the weather.
     *
     * @param state The current state of the workflow.
     * @return A map containing travel recommendations as output.
     */
    Map<String, Object> callTravelAgent(State state) {
        log.info("callTravelAgent: {}", state);

        var weather = (String) state.getOutput().get("weather");

        String recommendation;
        if (weather.contains("rain") || weather.contains("雨")) {
            recommendation = "Visit indoor attractions like museums, art galleries, or enjoy a day at the mall.";
        } else if (weather.contains("cloudy") || weather.contains("阴")) {
            recommendation = "Consider activities like visiting an aquarium, a science center, or enjoying a cozy café.";
        } else if (weather.contains("storm") || weather.contains("thunder") || weather.contains("雷") || weather.contains("冰雹")) {
            recommendation = "Stay safe indoors. Enjoy a good book, watch a movie, or try indoor yoga.";
        } else if (weather.contains("snow") || weather.contains("雪")) {
            recommendation = "Explore indoor winter activities like skating in an indoor rink or sipping hot chocolate by a fireplace.";
        } else {
            recommendation = "Enjoy outdoor activities like hiking, biking, or a picnic in the park!";
        }

        Map<String, Object> output = new HashMap<>();
        output.put("recommendation", recommendation);
        log.info("Travel Agent Output: {}", output);

        return Map.of(State.TRAVEL, output);
    }

    /**
     * Calls the Food Agent to provide food suggestions based on travel recommendations.
     *
     * @param state The current state of the workflow.
     * @return A map containing food suggestions as output.
     */
    Map<String, Object> callFoodAgent(State state) {
        log.info("callFoodAgent: {}", state);

        var recommendation = (String) state.getMID().get("recommendation");

        String foodSuggestion;
        if (recommendation.contains("outdoor")) {
            foodSuggestion = "Pack some easy-to-carry snacks like sandwiches, granola bars, and fresh fruit. Don't forget plenty of water!";
        } else if (recommendation.contains("cloudy")) {
            foodSuggestion = "Warm up with comfort food like soups, hot beverages, or enjoy a cozy brunch at a nearby bakery.";
        } else if (recommendation.contains("snow") || recommendation.contains("rain") || recommendation.contains("misty")) {
            foodSuggestion = "Enjoy hearty meals like stews, hot chocolate, or baked goods to keep you warm.";
        } else {
            foodSuggestion = "Explore the local street food scene or grab a quick bite from food trucks in the area.";
        }

        Map<String, Object> output = new HashMap<>();
        output.put("food", foodSuggestion);
        log.info("Food Agent Output: {}", output);

        return Map.of(State.FOOD, output);
    }

    /**
     * Provides a builder to construct the workflow graph.
     *
     * @return An instance of GraphBuilder.
     */
    public GraphBuilder graphBuilder() {
        return new GraphBuilder();
    }

    /**
     * Represents the state of the workflow, including input, intermediate, and output data.
     */
    public static class State extends AgentState {
        public static final String INPUT = "question";
        public static final String WEATHER = "weather";
        public static final String TRAVEL = "recommendation";
        public static final String FOOD = "food";

        static Map<String, Channel<?>> SCHEMA = Map.of(
                INPUT, Channels.base(() -> new HashMap<>()),
                WEATHER, Channels.base(() -> new HashMap<>()),
                TRAVEL, Channels.base(() -> new HashMap<>())
        );

        /**
         * Constructor to initialize state with given data.
         *
         * @param initData Initial data for the state.
         */
        public State(Map<String, Object> initData) {
            super(initData);
        }

        public Map<String, Object> getInput() {
            return this.<Map<String, Object>>value(INPUT).orElseGet(HashMap::new);
        }

        public Map<String, Object> getOutput() {
            return this.<Map<String, Object>>value(WEATHER).orElseGet(HashMap::new);
        }

        public Map<String, Object> getMID() {
            return this.<Map<String, Object>>value(TRAVEL).orElseGet(HashMap::new);
        }
    }

    /**
     * Builder class to construct a StateGraph for the agent workflow.
     */
    public class GraphBuilder {

        /**
         * Builds the workflow graph by defining nodes and transitions.
         *
         * @return The constructed StateGraph.
         * @throws GraphStateException If the graph cannot be constructed.
         */
        public StateGraph<State> build() throws GraphStateException {
            var shouldContinue = (EdgeAction<State>) state -> {
                log.info("shouldContinue state: {}", state);
                // 如果输入中，要求我们进行推荐，则继续进行旅游项目和食品的推荐
                return state.getInput().containsKey("recommendations") || state.getInput().containsKey("推荐")
                        ? "travelAgent" : "end";
            };

            return new StateGraph<>(State.SCHEMA, State::new)
                    .addEdge(START, "weatherAgent")
                    .addNode("weatherAgent", node_async(DemoAgentExecutor.this::callWeatherAgent))
                    .addNode("travelAgent", node_async(DemoAgentExecutor.this::callTravelAgent))
                    .addNode("foodAgent", node_async(DemoAgentExecutor.this::callFoodAgent))
                    .addConditionalEdges("weatherAgent",
                            edge_async(shouldContinue),
                            Map.of(
                                    "travelAgent", "travelAgent",
                                    "end", END
                            )
                    )
                    .addEdge("travelAgent", "foodAgent")
                    .addEdge("foodAgent", END);
        }
    }
}
