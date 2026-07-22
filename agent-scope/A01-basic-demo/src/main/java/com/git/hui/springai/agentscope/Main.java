package com.git.hui.springai.agentscope;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
import io.agentscope.harness.agent.HarnessAgent;

import java.nio.file.Paths;

/**
 *
 * @author YiHui
 * @date 2026/7/22
 */
public class Main {
    public static void main(String[] args) {
        // 使用 Builder 模式构建 HarnessAgent
        HarnessAgent agent = HarnessAgent.builder()
                .name("assistant")                                  // Agent 名称
                .sysPrompt("你是一个有用的 AI 助手，名叫一灰灰。")    // 系统提示词 / 人设
                .model(
                        OpenAIChatModel.builder()
                                .apiKey(EnvUtil.getZhipuApiKey())
                                .baseUrl("https://open.bigmodel.cn/api/paas")
                                .endpointPath("/v4/chat/completions")
                                .modelName("GLM-4-Flash")
                                .stream(true)
                                .formatter(new OpenAIChatFormatter())
                                .build()
                )                       // 模型，自动读取 DASHSCOPE_API_KEY
                .workspace(Paths.get(".agentscope/workspace"))      // 工作区路径
                .build();

        // 构建运行时上下文（userId + sessionId）
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId("first-session")
                .userId("yihuihui")
                .build();

        // 第一轮对话：问好
        var response1 = agent.call(
                new UserMessage("你好！请介绍一下你自己。"), ctx
        ).block();
        System.out.println("=== 第一轮回复 ===");
        System.out.println(response1.getTextContent());

        // 第二轮对话：测试记忆是否持久化
        var response2 = agent.call(
                new UserMessage("我刚才问了你什么问题？"), ctx
        ).block();
        System.out.println("=== 第二轮回复 ===");
        System.out.println(response2.getTextContent());


//        流式输出
        agent.streamEvents(new UserMessage("帮我列出三条 Java 开发最佳实践。"), ctx)
                .doOnNext(event -> {
                    if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
                        // 模型返回的流式文本片段 —— 逐个字符输出
                        System.out.print(((TextBlockDeltaEvent) event).getDelta());
                    } else if (event.getType() == AgentEventType.TOOL_CALL_START) {
                        // Agent 即将调用工具
                        System.out.println("\n[tool] " +
                                ((ToolCallStartEvent) event).getToolCallName());
                    }
                })
                .blockLast();
    }
}