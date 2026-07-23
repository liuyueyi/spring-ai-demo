package com.git.hui.springai.agentscope;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.event.ToolCallDeltaEvent;
import io.agentscope.core.event.ToolCallEndEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.event.ToolResultStartEvent;
import io.agentscope.core.event.ToolResultTextDeltaEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author YiHui
 * @date 2026/7/22
 */
public class Main {
    public static void main(String[] args) {
        // 1. 创建 Toolkit 并注册工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherTool());

        // 2. 构建 ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name("weather-agent")
                .sysPrompt("你是一个天气查询助手。你可以查询某天的气温数据。" +
                        "当用户问温差、温度变化等问题时，请先查询各天的温度，再综合分析。")
                .model(OpenAIChatModel.builder()
                        .apiKey(EnvUtil.getZhipuApiKey())
                        .baseUrl("https://open.bigmodel.cn/api/paas")
                        .endpointPath("/v4/chat/completions")
                        .modelName("GLM-4-Flash")
                        .stream(true)
                        .formatter(new OpenAIChatFormatter())
                        .build())
                .toolkit(toolkit)
                .maxIters(15)        // 最大推理迭代次数
                .build();

        // 3. 流式监听 ReAct 全过程：思考 → 调用工具 → 观察结果 → 继续思考
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║      🔍 ReAct 思考推理过程              ║");
        System.out.println("╚════════════════════════════════════════╝\n");

        // 用于暂存正在组装的工具参数
        Map<String, StringBuilder> toolArgsBuffer = new ConcurrentHashMap<>();

        agent.streamEvents(new UserMessage("今天和昨天比降温了多少度？"))
                .doOnNext(event -> {
                    switch (event.getType()) {
                        case TOOL_CALL_START -> {
                            var e = (ToolCallStartEvent) event;
                            // 1. 此时只能获取到工具名称和唯一标识
                            System.out.println("\n[准备调用工具] " + e.getToolCallId() + " : " + e.getToolCallName());
                            // 初始化参数缓冲区
                            toolArgsBuffer.put(e.getToolCallId() + ":req", new StringBuilder());
                            toolArgsBuffer.put(e.getToolCallId() + ":rsp", new StringBuilder());
                        }
                        case TOOL_CALL_DELTA -> {
                            var e = (ToolCallDeltaEvent) event;
                            // 2. 通过 toolCallId 关联，流式拼接工具参数
                            StringBuilder buffer = toolArgsBuffer.get(e.getToolCallId() + ":req");
                            System.out.println("\n[工具参数] " + e.getToolCallId() + " : " + e.getDelta());
                            if (buffer != null) {
                                buffer.append(e.getDelta());
                            }
                        }
                        case TOOL_RESULT_START -> {
                            // 3. 工具开始执行
                            var e = (ToolResultStartEvent) event;
                            System.out.println("\n[工具执行开始] " + e.getToolCallId() + " : " + e.getToolCallName());
                        }
                        case TOOL_RESULT_TEXT_DELTA -> {
                            // 4. 流式获取工具返回结果
                            var e = (ToolResultTextDeltaEvent) event;
                            StringBuilder buffer = toolArgsBuffer.get(e.getToolCallId() + ":rsp");
                            System.out.println("[工具返回]: " + e.getDelta());
                            if (buffer != null) {
                                buffer.append(e.getDelta());
                            }
                        }
                        case TOOL_RESULT_END -> {
                            var e = (ToolResultEndEvent) event;
                            // 5. 参数拼接完成，获取完整参数并清理缓冲区
                            String finalArgs = toolArgsBuffer.remove(e.getToolCallId() + ":req").toString();
                            System.out.println("完整参数: " + finalArgs);
                            // 工具调用完成，获取完整结果并清理缓冲区
                            String finalResult = toolArgsBuffer.remove(e.getToolCallId() + ":rsp").toString();
                            System.out.println("完整结果: " + finalResult);
                        }
                    }
                })
                .blockLast();
    }
}