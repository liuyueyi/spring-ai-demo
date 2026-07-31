package com.git.hui.springai.agentscope;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.MemoryConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class BrainExternalizationDemo {

    public static void main(String[] args) throws Exception {
        // 指定工作区路径
        Path workspace = Paths.get(".agentscope/workspace");

        // 初始化 AGENTS.md（如果不存在）
        initWorkspaceIfAbsent(workspace);

        // 创建 HarnessAgent
        HarnessAgent agent = HarnessAgent.builder()
                .name("note-taker")
                .model(OpenAIChatModel.builder()
                        .apiKey(EnvUtil.getZhipuApiKey())
                        .baseUrl("https://open.bigmodel.cn/api/paas")
                        .endpointPath("/v4/chat/completions")
                        .modelName("GLM-4-Flash")
                        .stream(true)
                        .formatter(new OpenAIChatFormatter())
                        .build())
                .workspace(workspace)
                .memory(MemoryConfig.builder()
                        .build())
                .build();
//
//        // 第一轮对话：用户介绍自己
//        System.out.println("=== 第一轮对话 ===");
//        String reply1 = agent.call(
//                List.of(new UserMessage("你好！我叫张三，我在做一个电商平台的"
//                        + "微服务重构，用的 Spring Cloud Alibaba。")),
//                RuntimeContext.builder().sessionId("demo-session").build()
//        ).block().getTextContent();
//        System.out.println("Agent: " + reply1);
//
//        // 第二轮对话：看 Agent 是否记住了
//        System.out.println("\n=== 第二轮对话 ===");
//        String reply2 = agent.call(
//                List.of(new UserMessage("我还在做微服务重构吗？我的技术栈是什么？")),
//                RuntimeContext.builder().sessionId("demo-session").build()
//        ).block().getTextContent();
//        System.out.println("Agent: " + reply2);


        // 修改AGENTS.md之后，将简洁的回复调整为详细的回复，重新执行
        System.out.println("\n=== 第三轮对话 ===");
        String reply3 = agent.call(List.of(new UserMessage("帮我分析一下微服务拆分应该注意什么？")),
                RuntimeContext.builder().sessionId("demo-session").build())
                .block().getTextContent();
        System.out.println("Agent: " + reply3);

        // 打印 MEMORY.md 看看内容
        System.out.println("\n=== MEMORY.md 内容 ===");
        Path memoryMd = workspace.resolve("demo-session/MEMORY.md");
        if (Files.exists(memoryMd)) {
            Files.readAllLines(memoryMd).forEach(System.out::println);
        }
    }

    private static void initWorkspaceIfAbsent(Path workspace) throws Exception {
        Files.createDirectories(workspace);
        Path agentsMd = workspace.resolve("AGENTS.md");
        if (!Files.exists(agentsMd)) {
            Files.writeString(agentsMd, """
                    # 随身笔记助手

                    你是一个帮助用户整理笔记和知识的助手。

                    ## 行为准则
                    - 主动记录用户提到的关键事实（姓名、计划、偏好等）
                    - 在回答中使用这些信息，让对话更个性化
                    - 回复简洁，适当使用列表
                    """);
        }
    }
}