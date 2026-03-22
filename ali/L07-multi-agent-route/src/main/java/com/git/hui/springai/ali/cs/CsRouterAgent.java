package com.git.hui.springai.ali.cs;

import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 路由agent
 * @author YiHui
 * @date 2026/3/21
 */
@Component
@RequiredArgsConstructor
public class CsRouterAgent {

    private final ChatModel chatModel;
    private final SalesAgent salesAgent;
    private final HrAgent hrAgent;
    private final TechSupportAgent techSupportAgent;

    public LlmRoutingAgent routerAgent() {
        LlmRoutingAgent agent = LlmRoutingAgent.builder()
                .name("router_agent")
                .model(chatModel)
                .description("根据用户输入，将用户问题路由到对应的业务部门")
                .instruction("""
                        根据用户输入，将用户问题路由到对应的业务部门。
                                                
                        【业务部门】
                        销售部门：处理销售信息、产品介绍、购买渠道、优惠活动等等销售问题
                        人力资源部门：处理招聘信息、投递简历、面试安排等人力资源问题
                        技术支持部门：处理技术问题、bug修复、代码优化等等技术问题
                        """
                ).subAgents(
                        List.of(salesAgent.salesAgent(),
                                hrAgent.hrAgent(),
                                techSupportAgent.techSupportAgent())
                ).build();
        return agent;
    }

}
