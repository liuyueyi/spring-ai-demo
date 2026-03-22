package com.git.hui.springai.ali.cs;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 *
 * @author YiHui
 * @date 2026/3/21
 */
@Component
@RequiredArgsConstructor
public class TechSupportAgent {
    private final ChatModel chatModel;

    public ReactAgent techSupportAgent() {
        ReactAgent agent = ReactAgent.builder()
                .name("tech_support_agent")
                .model(chatModel)
                .description("处理产品故障、使用指导、技术咨询等问题。")
                .instruction("你是一个专业Tech Support，擅长各种技术问题，包括产品故障处理、使用指导、技术咨询等，请根据用户的提问进行回答")
                .outputKey("tech_response")
                .enableLogging(true)
                .build();
        return agent;
    }
}
