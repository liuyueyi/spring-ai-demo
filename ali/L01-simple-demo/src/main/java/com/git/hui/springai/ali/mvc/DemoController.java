package com.git.hui.springai.ali.mvc;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;

/**
 *
 * @author YiHui
 * @date 2026/3/9
 */
@RestController
public class DemoController {
    @Autowired
    private ChatModel chatModel;

    @RequestMapping(path = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE + "; charset=utf-8")
    public Flux ask(String type, String msg) throws GraphRunnerException {
        return switch (type) {
            case "instruction":
                yield instructionUsage(msg);
            case "weather":
                yield toolUsage(msg);
            default:
                yield Flux.just("no function call!");
        };
    }

    public Flux instructionUsage(String msg) throws GraphRunnerException {
        String instruction = """
                你是一个经验丰富的软件架构师。
                				
                在回答问题时，请：
                1. 首先理解用户的核心需求
                2. 分析可能的技术方案
                3. 提供清晰的建议和理由
                4. 如果需要更多信息，主动询问
                				
                保持专业、友好的语气。
                """;

        ReactAgent agent = ReactAgent.builder()
                .name("architect_agent")
                .model(chatModel)
                .instruction(instruction)
                .build();

        // 同步调用，并获取历史的返回结果
        Optional<OverAllState> result = agent.invoke(msg);
        StringBuilder ans = new StringBuilder();
        if (result.isPresent()) {
            // 访问历史消息
            List<Message> messages = (List<Message>) result.get().value("messages").orElse(List.of());
            for (Message message : messages) {
                if (message instanceof AssistantMessage) {
                    ans.append(message.getText());
                }
            }
        }
        return Flux.just(Map.of("content", ans.toString()));
    }

    // 定义天气查询工具
    public static class WeatherTool implements BiFunction<Map<String, Object>, ToolContext, String> {
        @Override
        public String apply(@ToolParam(description = "城市名，如：武汉") Map<String, Object> params,
                            ToolContext toolContext) {
            String city = (String) params.get("city");
            System.out.println("[WeatherTool] Query: " + city);
            if (city == null || city.isEmpty()) {
                return "错误：未提供城市名称";
            }
            return "It's always sunny in " + city + "!";
        }
    }

    // 用户位置工具 - 使用上下文
    public class UserLocationTool implements BiFunction<Map, ToolContext, String> {
        @Override
        public String apply(
                @ToolParam(description = "用户查询") Map query,
                ToolContext toolContext) {
            // 从上下文中获取用户信息
            String userId = "";
            if (toolContext != null && toolContext.getContext() != null) {
                RunnableConfig runnableConfig = (RunnableConfig) toolContext.getContext().get(AGENT_CONFIG_CONTEXT_KEY);
                Optional<Object> userIdObjOptional = runnableConfig.metadata("user_id");
                if (userIdObjOptional.isPresent()) {
                    userId = (String) userIdObjOptional.get();
                }
            }
            if (userId == null) {
                userId = "1";
            }
            System.out.println("[UserLocationTool] Query: " + query + " -> " + userId);
            return "1".equals(userId) ? "武汉" : "上海";
        }
    }

    public Flux<Map<String, String>> toolUsage(String msg) throws GraphRunnerException {
        // 工具注册
        ToolCallback weatherTool = FunctionToolCallback.builder("get_weather", new WeatherTool())
                .description("根据你传入的城市，返回对应的天气")
                .inputType(Map.class)
                .build();

        ToolCallback userLocationTool = FunctionToolCallback
                .builder("get_user_location", new UserLocationTool())
                .description("根据用户id查询用户所处的城市名")
                .inputType(Map.class)
                .build();

        // 创建 agent
        ReactAgent agent = ReactAgent.builder()
                .name("weather_agent")
                .model(chatModel)
                .tools(weatherTool, userLocationTool)
                .systemPrompt("""
                        你现在是一个智能天气助手。当用户询问天气或提到城市时，请调用 get_weather 工具查询天气。
                        如果你不知道具体的城市信息，请首先调用 get_user_location 工具查询用户所在的城市，然后再查询对应的天气信息返回
                        注意：没有具体城市时，直接调用工具获取城市，不需要二次确认
                        """)
                .saver(new MemorySaver())
                .build();

        // threadId 是给定对话的唯一标识符
        String threadId = "1";
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).addMetadata("user_id", "1").build();

        // 运行 agent
        // 第一次调用
        System.out.println("开始进入调用：");
        Flux<NodeOutput> response = agent.stream(msg, runnableConfig);

        // 处理 NodeOutput，提取有用的响应内容
        return response.map(nodeOutput -> {
//            System.out.println("NodeOutput: " + nodeOutput);
            if (nodeOutput instanceof StreamingOutput streamingOutput) {
                OutputType outputType = streamingOutput.getOutputType();
                System.out.println("OutputType: " + outputType + "\t => " + streamingOutput.message().getText());
                String tag;
                if (outputType == OutputType.AGENT_MODEL_STREAMING) {
                    // 流式消息，增量返回
                    tag = nodeOutput.node() + "#delta:";
                } else if (outputType == OutputType.AGENT_MODEL_FINISHED) {
                    // 流式消息执行完成，返回完整的结果
                    tag = nodeOutput.node() + "#complete:";
                } else if (outputType == OutputType.AGENT_TOOL_FINISHED) {
                    // 工具执行结果
                    tag = nodeOutput.node() + "#tool_result:";
                    ToolResponseMessage toolMsg = (ToolResponseMessage) ((StreamingOutput<?>) nodeOutput).message();
                    List<ToolResponseMessage.ToolResponse> responses = toolMsg.getResponses();
                    StringBuilder toolRes = new StringBuilder();
                    for (ToolResponseMessage.ToolResponse rp : responses) {
                        toolRes.append("Tool:" + rp.name()).append(" ==rsp=> ").append(rp.responseData());
                    }
                    return Map.of("content", tag + toolRes);
                } else {
                    // 因为ReAct底层是基于Graph实现，因此除了 _AGENT_MODEL_ 节点之外，必然还存在 __START__ node 和 __END__ node
                    tag = nodeOutput.node() + "#" + outputType.name() + ":";
                }
                return Map.of("content", tag + streamingOutput.message().getText());
            } else if (nodeOutput.isSTART()) {
                System.out.println("START");
                return Map.of("content", "START");
            } else if (nodeOutput.isEND()) {
                System.out.println("END");
                return Map.of("content", "END");
            }
            return Map.of("content", "No response");
        });
    }

}
