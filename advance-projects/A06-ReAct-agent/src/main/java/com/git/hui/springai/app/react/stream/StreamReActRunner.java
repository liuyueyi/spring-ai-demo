package com.git.hui.springai.app.react.stream;

import com.git.hui.springai.app.react.simple.CalculatorTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 流式 ReAct Agent 演示应用
 */
//@Component
public class StreamReActRunner implements CommandLineRunner {

    private final ChatClient chatClient;

    public StreamReActRunner(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. 准备工具
        CalculatorTools calculatorTools = new CalculatorTools();
        List<ToolCallback> tools = calculatorTools.getTools();

        // 2. 创建流式 ReAct Agent
        StreamReActAgent agent = new StreamReActAgent(chatClient, tools);

        // 3. 运行示例
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║      🌊 流式 ReAct Agent 演示           ║");
        System.out.println("╚════════════════════════════════════════╝\n");

        System.out.println("========== 示例 1: 简单加法 ==========");
        String answer1 = agent.run("计算 25 + 37 等于多少？");
        System.out.println("最终结果：" + answer1);

        System.out.println("\n========== 示例 2: 多步计算 ==========");
        String answer2 = agent.run("先计算 100 + 50，然后将结果乘以 2，最后除以 3；\n最终根据上面这个计算返回的结果，是奇数就查询武汉天气、是偶数则查询北京天气");
        System.out.println("最终结果：" + answer2);

        System.out.println("\n========== 示例 3: 混合运算 ==========");
        String answer3 = agent.run("计算 (80 - 20) * 3 / 4 的结果");
        System.out.println("最终结果：" + answer3);

        // 示例 3: 复杂应用题
        System.out.println("\n========== 示例 4: 复杂应用题 ==========");
        String answer4 = agent.run("小明有 50 元钱，买了 3 本书，每本书 12 元，还剩多少钱？");
        System.out.println("\n最终答案：" + answer4);
        System.out.println("\n----------------------------------------\n");
    }
}
