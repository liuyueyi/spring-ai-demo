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
public class SalesAgent {
    private final ChatModel chatModel;

    public ReactAgent salesAgent() {
        ReactAgent agent = ReactAgent.builder()
                .name("sales_agent")
                .model(chatModel)
                .description("处理销售信息、产品介绍、购买渠道、优惠活动等等销售问题")
                .instruction("你是一个销售专家，擅长各种销售、产品介绍、购买渠道、优惠活动等等销售问题")
                .outputKey("sales_response")
                .enableLogging(true)
                .build();
        return agent;
    }
}
