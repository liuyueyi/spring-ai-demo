package com.git.hui.springai.app.executor;

import com.git.hui.springai.app.agents.GenCardImgAgent;
import com.git.hui.springai.app.agents.GenCardImgPromptGenAgent;
import com.git.hui.springai.app.agents.GenCardWordsAgent;
import com.git.hui.springai.app.model.CardReq;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * @author YiHui
 * @date 2025/8/15
 */
@Service
public class AgentExecutor {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AgentExecutor.class);
    private GenCardImgAgent genCardImgAgent;
    private GenCardWordsAgent genCardWordsAgent;
    private GenCardImgPromptGenAgent genCardImgPromptGenAgent;
    private final CompiledGraph<TxtImgAgentState> compiledGraph;

    public AgentExecutor(GenCardImgAgent genCardImgAgent,
                         GenCardWordsAgent genCardWordsAgent,
                         GenCardImgPromptGenAgent genCardImgPromptGenAgent) throws GraphStateException {
        this.genCardImgAgent = genCardImgAgent;
        this.genCardWordsAgent = genCardWordsAgent;
        this.genCardImgPromptGenAgent = genCardImgPromptGenAgent;
        this.compiledGraph = new GraphBuilder().build().compile();
        this.printPlantUml();
    }

    public TxtImgAgentState invoke(CardReq input) {
        return this.compiledGraph
                .invoke(Map.of(TxtImgAgentState.INPUT_TEXT, input))
                .orElseGet(() -> new TxtImgAgentState(Map.of("Error", "NoDataResponse")));
    }

    public class GraphBuilder {
        public StateGraph<TxtImgAgentState> build() throws GraphStateException {
            return new StateGraph<>(TxtImgAgentState.serializer())
                    .addNode(GenCardWordsAgent.AGENT_NAME, node_async(genCardWordsAgent::apply))
                    .addNode(GenCardImgPromptGenAgent.AGENT_NAME, node_async(genCardImgPromptGenAgent::apply))
                    .addNode(GenCardImgAgent.AGENT_NAME, node_async(genCardImgAgent::apply))
                    .addEdge(START, GenCardWordsAgent.AGENT_NAME)
                    .addEdge(GenCardWordsAgent.AGENT_NAME, GenCardImgPromptGenAgent.AGENT_NAME)
                    .addEdge(GenCardImgPromptGenAgent.AGENT_NAME, GenCardImgAgent.AGENT_NAME)
                    .addEdge(GenCardImgAgent.AGENT_NAME, END);
        }
    }


    /**
     * 打印 plantUml 格式流程图
     *
     * @return
     */
    private String printPlantUml() {
        // 在线 mermaid绘制地址：https://mermaid.live/
//        GraphRepresentation representation = compiledGraph.getGraph(GraphRepresentation.Type.MERMAID, "TravelRecommendAgent", true);

        // 在线uml绘制地址： https://www.plantuml.com/plantuml/uml/SyfFKj2rKt3CoKnELR1Io4ZDoSa700002
        GraphRepresentation representation = compiledGraph.getGraph(GraphRepresentation.Type.PLANTUML, "TravelRecommendAgent", true);
        // 获取 PlantUML 文本
        System.out.println(">>>>>>>>>>>> online uml render site:  https://www.plantuml.com/plantuml/uml/SyfFKj2rKt3CoKnELR1Io4ZDoSa700002");
        System.out.println("=== PlantUML Start ===");
        System.out.println(representation.content());
        System.out.println("------- PlantUML End ---------");
        return representation.content();
    }
}
