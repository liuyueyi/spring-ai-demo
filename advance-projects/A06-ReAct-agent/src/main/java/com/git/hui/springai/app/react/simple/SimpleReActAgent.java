package com.git.hui.springai.app.react.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * 轻量级 ReAct Agent 实现
 * 核心原理：Thinking -> Act -> Observe 循环
 *
 * @author YiHui
 * @date 2026/3/4
 */
public class SimpleReActAgent {
    private static final Logger log = LoggerFactory.getLogger(SimpleReActAgent.class);

    // 最大迭代次数
    private static final int MAX_ITERATIONS = 10;

    private final ChatClient chatClient;
    private final List<ToolCallback> tools;

    public SimpleReActAgent(ChatClient chatClient, List<ToolCallback> tools) {
        this.chatClient = chatClient;
        this.tools = tools != null ? tools : new ArrayList<>();
    }

    /**
     * 运行 ReAct 循环
     * @param question 用户问题
     * @return 最终答案
     */
    public String run(String question) {
        // 1. 初始化对话历史
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(question));

        log.info("╔════════════════════════════════════════╗");
        log.info("║       🚀 ReAct Agent 启动              ║");
        log.info("╚════════════════════════════════════════╝");
        log.info("💬 问题：{}", question);

        // 2. ReAct 循环
        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
            log.info("┌────────────────────────────────────────");
            log.info("│ 🔁 第 {} 轮思考", iteration);
            log.info("└────────────────────────────────────────");

            try {
                // Thinking: 让大模型思考下一步该做什么
                ChatResponse response = think(messages);
                AssistantMessage assistantMessage = response.getResult().getOutput();

                // Act & Observe: 检查是否需要调用工具
                if (hasToolCalls(assistantMessage)) {
                    // 执行工具调用
                    String toolResult = executeTools(assistantMessage);

                    // 将工具结果添加到对话历史
                    var toolCall = getToolCall(assistantMessage);
                    messages.add(ToolResponseMessage.builder().responses(
                            List.of(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), toolResult)
                            )
                    ).build());

                    log.info("工具执行结果 {} => {}", toolCall.name(), toolResult);

                } else {
                    // 没有工具调用，说明大模型已给出最终答案
                    String finalAnswer = assistantMessage.getText();
                    log.info(" ✅ 无工具调用，直接返回结果：{}", finalAnswer);

                    log.info("╔════════════════════════════════════════╗");
                    log.info("║       ✨ ReAct 任务完成 ✨              ║");
                    log.info("╚════════════════════════════════════════╝\n");
                    return finalAnswer;
                }

            } catch (Exception e) {
                log.error("ReAct 循环出错：{}", e.getMessage(), e);
                break;
            }
        }

        log.info("╔════════════════════════════════════════╗");
        log.info("║ ⚠️  达到最大迭代次数，任务终止          ║");
        log.info("╚════════════════════════════════════════╝\n");
        return "达到最大迭代次数 (" + MAX_ITERATIONS + ")，无法完成任务。";
    }

    /**
     * Thinking 阶段 - 让大模型思考下一步
     */
    private ChatResponse think(List<Message> messages) {
        // 构建系统提示
        String systemPrompt = """
                你是一个智能助手，使用 ReAct 范式解决问题。
                                
                请按以下步骤思考：
                1. 分析当前问题和已有信息
                2. 判断是否需要使用工具获取更多信息
                3. 如果需要工具，调用一个合适的工具
                4. 【重要】如果已有足够信息，直接给出最终答案
                                
                响应规则：
                - 你必须一次只调用一个工具，不允许同时调用多个工具
                - 关键：调用工具时，你必须使用工具定义中完整的工具名称（例如："fs-read-file-operator"）
                - 【重要】如果之前的工具执行已经提供了足够的信息来回答原始问题，不要再调用另一个工具。相反，综合结果并直接提供你的最终答案
                                
                可用工具列表：\n""" + getToolDescriptions();

        Message systemMessage = new org.springframework.ai.chat.messages.SystemMessage(systemPrompt);
        List<Message> allMessages = new ArrayList<>();
        allMessages.add(systemMessage);
        allMessages.addAll(messages);

        // 设置工具调用选项
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)  // 禁用自动执行，由我们手动控制
                .build();

        Prompt prompt = new Prompt(allMessages, options);

        return chatClient.prompt(prompt)
                .toolCallbacks(tools)
                .call()
                .chatResponse();
    }

    /**
     * 检查是否有工具调用
     */
    private boolean hasToolCalls(AssistantMessage message) {
        return message.getToolCalls() != null && !message.getToolCalls().isEmpty();
    }

    /**
     * 获取第一个工具的名称
     */
    private AssistantMessage.ToolCall getToolCall(AssistantMessage message) {
        if (hasToolCalls(message)) {
            return message.getToolCalls().get(0);
        }
        return null;
    }

    /**
     * Act & Observe 阶段 - 执行工具并观察结果
     */
    private String executeTools(AssistantMessage message) throws Exception {
        var toolCall = getToolCall(message);  // 只处理第一个工具调用

        log.debug("  ┌─ 🔧 执行工具调用");
        log.debug("  │   工具名称：{}", toolCall.name());
        log.debug("  │   工具参数：{}", toolCall.arguments());

        // 查找并执行匹配的工具
        for (ToolCallback tool : tools) {
            if (tool.getToolDefinition().name().equals(toolCall.name())) {
                Object result = tool.call(toolCall.arguments());
                String resultText = result != null ? result.toString() : "null";
                log.debug("  │   执行结果：{}", resultText);
                log.debug("  └─ ✓ 工具执行完成\n");
                return resultText;
            }
        }

        throw new RuntimeException("未找到工具：" + toolCall.name());
    }

    /**
     * 获取工具描述信息
     */
    private String getToolDescriptions() {
        if (tools.isEmpty()) {
            return "无";
        }

        StringBuilder sb = new StringBuilder();
        for (ToolCallback tool : tools) {
            sb.append("- ").append(tool.getToolDefinition().name())
                    .append(": ").append(tool.getToolDefinition().description())
                    .append("\n");
        }
        return sb.toString();
    }
}
