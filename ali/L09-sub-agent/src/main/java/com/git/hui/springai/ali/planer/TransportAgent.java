package com.git.hui.springai.ali.planer;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.renderer.SaaStTemplateRenderer;
import com.git.hui.springai.ali.hook.HookFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 交通规划专家
 * @author YiHui
 * @date 2026/3/22
 */
@RequiredArgsConstructor
@Component
public class TransportAgent {
    private final ChatModel chatModel;

    public ReactAgent agent() {
        ReactAgent agent = ReactAgent.builder()
                .name("transport_agent")
                .model(chatModel)
                .description("交通规划专家，善于根据用户需求规划最佳的出行方式")
                .systemPrompt("""
                        你是一个交通规划专家。请根据用户的出发地、目的地、景点安排，规划交通方式。
                        请返回详细的交通方案，包括城际交通和市内交通建议。
                          """
                )
                .enableLogging(true)
                .inputSchema("""
                        {
                             "input": {
                                 "type": "string",
                             }
                         }
                        """)
                .hooks(HookFactory.createLogAgentHook())
                .outputKey("transport_plan")
                .build();
        return agent;
    }

}
