package com.git.hui.springai.ali.planer;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author YiHui
 * @date 2026/3/17
 */
@Service
public class ExecutionAgent {
    private static final String PROMPT = """
            你是一个活动执行专家，请为以下活动主题规划关键的执行步骤和时间节点。
            活动主题：{input}
            直接返回执行步骤清单。
              """;

    @Autowired
    private ChatModel chatModel;

    public ReactAgent executionAgent() {
        ReactAgent agent = ReactAgent.builder()
                .name("execution_agent")
                .model(chatModel)
                .description("执行专家Agent")
                .instruction(PROMPT)
                .outputKey("execution_steps")
                .includeContents(false)
                .returnReasoningContents(false)
                .enableLogging(true)
                .build();
        return agent;
    }
}
