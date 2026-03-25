package com.git.hui.springai.ali.planer;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.renderer.SaaStTemplateRenderer;
import com.git.hui.springai.ali.hook.HookFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 *
 * 住宿推荐专家
 * @author YiHui
 * @date 2026/3/22
 */
@RequiredArgsConstructor
@Component
public class HotelAgent {
    private final ChatModel chatModel;

    public ReactAgent agent() {
        ReactAgent agent = ReactAgent.builder()
                .name("hotel_agent")
                .model(chatModel)
                .description("住宿推荐专家，善于根据用户需求推荐适合的住宿")
                .systemPrompt("""
                        你是一个住宿推荐专家。请根据用户的景点计划、预算，推荐合适的住宿地点和酒店。
                        请返回住宿建议，包括区域和酒店示例。
                        """
                )
                .inputSchema("""
                        {
                             "input": {
                                 "type": "string",
                             }
                         }
                        """)
                .templateRenderer(SaaStTemplateRenderer.builder().build())
                .outputKey("hotel_plan")
                .hooks(HookFactory.createLogAgentHook())
                .enableLogging(true)
                .build();
        return agent;
    }
}
