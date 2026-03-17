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
public class BudgetAgent {
    private static final String PROMPT = """
            你是一个财务规划师，请为以下活动主题制定一个粗略的预算规划，列出主要开支项和预估费用。
            活动主题：{input}
            直接返回预算规划内容。
             """;

    @Autowired
    private ChatModel chatModel;

    public ReactAgent budgetAgent() {
        ReactAgent agent = ReactAgent.builder()
                .name("budget_agent")
                .model(chatModel)
                .description("财务预算规划Agent")
                .instruction(PROMPT)
                .outputKey("budget_plan")
                .includeContents(false)
                .returnReasoningContents(false)
                .enableLogging(true)
                .build();
        return agent;
    }
}
