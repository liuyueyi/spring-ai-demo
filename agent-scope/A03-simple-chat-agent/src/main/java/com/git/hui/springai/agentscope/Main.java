package com.git.hui.springai.agentscope;

import com.git.hui.springai.EnvUtil;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.event.ToolCallDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultTextDeltaEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;

import java.util.Scanner;

/**
 *
 * @author YiHui
 * @date 2026/7/23
 */
public class Main {
    public static void main(String[] args) {
        if (true) {
            reasonAgent();
            return;
        }

        // 1. 构建模型（开启流式模式）
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(EnvUtil.getSiliconApiKey())
                .baseUrl("https://api.siliconflow.cn")
                .endpointPath("/v1/chat/completions")
                .modelName("Qwen/Qwen3-8B")
                .stream(true)                    // ← 流式输出开关
                .formatter(new OpenAIChatFormatter())
                .build();

        // 2. 构建 Agent（纯对话，不需要工具）
        ReActAgent agent = ReActAgent.builder()
                .name("chat-bot")
                .sysPrompt("你是一个友善、风趣的聊天伙伴。" +
                        "你会记住对话历史，基于上下文回答问题。" +
                        "回答简洁但信息量充足。")
                .model(model)
                .maxIters(3)                     // 纯对话不需要太多迭代
                .build();

        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║      🤖 一灰灰聊天机器人 v1.0       ║");
        System.out.println("║  输入 'exit' 退出，输入 'clear' 清空  ║");
        System.out.println("╚════════════════════════════════════╝\n");

        // 3. 多轮对话循环
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("🙋 你: ");
                String input = scanner.nextLine().trim();

                if ("exit".equalsIgnoreCase(input)) {
                    System.out.println("👋 再见！");
                    break;
                }
                if ("clear".equalsIgnoreCase(input)) {
                    agent = ReActAgent.builder()
                            .name("chat-bot")
                            .sysPrompt("你是一个友善、风趣的聊天伙伴。" +
                                    "你会记住对话历史，基于上下文回答问题。" +
                                    "回答简洁但信息量充足。")
                            .model(model)
                            .maxIters(3)
                            .build();            // ← 重新创建 Agent 清空上下文
                    System.out.println("🧹 对话历史已清空！\n");
                    continue;
                }

                System.out.print("🤖 Bot: ");
                // 流式输出——逐字打印
                agent.streamEvents(new UserMessage(input))
                        .doOnNext(event -> {
                            switch (event.getType()) {
                                case AGENT_START -> {
                                    // Agent 开始处理
                                    System.out.println("\n🤔 正在思考...");
                                }
                                case THINKING_BLOCK_DELTA -> {
                                    // 思考过程——灰色显示思维链
                                    String thought = ((ThinkingBlockDeltaEvent) event).getDelta();
                                    System.out.print("\u001B[90m" + thought + "\u001B[0m"); // 灰色
                                }
                                case THINKING_BLOCK_END -> {
                                    // 思考结束，准备输出
                                    System.out.println("\n\u001B[90m━━━ 思考完毕，开始回答 ━━━\u001B[0m\n");
                                }
                                case TEXT_BLOCK_DELTA -> {
                                    // 正式回答——正常颜色流式输出
                                    System.out.print(((TextBlockDeltaEvent) event).getDelta());
                                }
                                case TOOL_CALL_START -> {
                                    // Agent 调用工具
                                    String toolName = ((ToolCallStartEvent) event).getToolCallName();
                                    System.out.println("\n🔧 [调用工具] " + toolName);
                                }
                                case TOOL_CALL_DELTA -> {
                                    // 工具执行传参
                                    String toolInput = ((ToolCallDeltaEvent) event).getDelta();
                                    System.out.println("➡️  参数: " + toolInput);
                                }
                                case TOOL_RESULT_TEXT_DELTA -> {
                                    // 工具执行结果流式输出
                                    String toolOutput = ((ToolResultTextDeltaEvent) event).getDelta();
                                    System.out.print("➡️  返回: " + toolOutput);
                                }
                                case TOOL_RESULT_END -> {
                                    // 工具执行完毕
                                    System.out.println("\n✅ [工具完成]");
                                }
                                case AGENT_END -> {
                                    // 全部处理完成
                                    System.out.println("\n\n✅ 完成！");
                                }
                                default -> {
                                    // 其他事件忽略
                                }
                            }
                        })
                        .blockLast();
                System.out.println("\n");
            }
        }
    }


    private static void reasonAgent() {
        // 使用推理模型（支持原生思维链）
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(EnvUtil.getSiliconApiKey())
                .baseUrl("https://api.siliconflow.cn")
                .endpointPath("/v1/chat/completions")
                .modelName("Qwen/Qwen3-8B")
                .stream(true)                    // ← 流式输出开关
                .formatter(new OpenAIChatFormatter())
                .build();
        // ToolKit 添加网络搜索工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WebSearchTool());


        // 构建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("deep-thinker")
                .sysPrompt("你是一个深度思考助手。面对复杂问题，你会：\n" +
                        "1. 先展示完整的推理过程\n" +
                        "2. 如果需要实时信息，调用搜索工具\n" +
                        "3. 给出最终答案时引用推理中的关键结论")
                .model(model)
                .toolkit(toolkit)
                .maxIters(10)
                .build();

        // 发起查询——展示完整思考 + 搜索 + 回答过程
        agent.streamEvents(new UserMessage(
                        "AgentScope Java 2.0 相比 1.x 版本有哪些重要的架构改进？ 官方文档地址为：https://java.agentscope.io/v2/zh/docs/index.html"))
                .doOnNext(event -> {
                    switch (event.getType()) {
                        case AGENT_START -> System.out.println("🚀 开始分析问题...\n");

                        case THINKING_BLOCK_START -> System.out.println("┌─ 🤔 深度思考中 ─────────────────────┐");

                        case THINKING_BLOCK_DELTA -> {
                            // 思考内容——用灰色斜体展示
                            String thought = ((ThinkingBlockDeltaEvent) event).getDelta();
                            System.out.print("\u001B[90m" + thought + "\u001B[0m");
                        }

                        case THINKING_BLOCK_END -> System.out.println("\n└────────────────────────────────────┘\n");

                        case TOOL_CALL_START -> {
                            String toolName = ((ToolCallStartEvent) event).getToolCallName();
                            System.out.println("🔍 [正在搜索] " + toolName + " ...");
                        }

                        case TOOL_RESULT_TEXT_DELTA -> {
                            String toolOutput = ((ToolResultTextDeltaEvent) event).getDelta();
                            System.out.println("\n\u001B[90m━━━ 搜索结果 ━━━\u001B[0m\n" + toolOutput);
                        }

                        case TEXT_BLOCK_DELTA -> System.out.print(((TextBlockDeltaEvent) event).getDelta());

                        case AGENT_END -> System.out.println("\n\n✅ 分析完成！");
                    }
                })
                .blockLast();
    }
}