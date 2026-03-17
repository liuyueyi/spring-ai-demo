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
public class CreativeAgent {
    private static final String PROMPT = """
            你是一个创意策划师，请为以下活动主题提出至少3个富有创意的点子，要求新颖、可行。
            活动主题：{input}
            直接返回创意列表，每一点用简短的话描述。
             """;

    @Autowired
    private ChatModel chatModel;

    public ReactAgent creativeAgent() {
        ReactAgent agent = ReactAgent.builder()
                .name("creative_agent")
                .model(chatModel)
                .description("创意生成Agent")
                .instruction(PROMPT)
                .outputKey("creative_ideas")
                .includeContents(false)
                .returnReasoningContents(false)
                .enableLogging(true)
                .build();
        return agent;
    }
}
