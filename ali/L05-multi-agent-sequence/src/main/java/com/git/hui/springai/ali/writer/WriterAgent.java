package com.git.hui.springai.ali.writer;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author YiHui
 * @date 2026/3/17
 */
@Component
public class WriterAgent {

    private static final String instruction = """
            你是一个知名的作家，擅长用生动、易懂的语言创作文章。
            
            ---
            请根据大纲进行完整的内容创作。大纲内容如下：
            {outline}
            
            ---
            直接返回文章内容，不要包含任何额外说明。
            用户的提问是: 
            {input}
            """;
    @Autowired
    private ChatModel chatModel;

    public ReactAgent writerAgent() {
        ReactAgent agent = ReactAgent.builder()
                .name("write_agent")
                .model(chatModel)
                .description("专业写作Agent")
                .instruction(instruction)
                .outputKey("article_draft")
                .includeContents(false)
                .returnReasoningContents(false)
                .enableLogging(true)
                .build();
        return agent;
    }
}
