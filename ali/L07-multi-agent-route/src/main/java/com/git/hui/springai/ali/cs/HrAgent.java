package com.git.hui.springai.ali.cs;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author YiHui
 * @date 2026/3/21
 */
@Component
public class HrAgent {
    @Autowired
    private ChatModel chatModel;

    public ReactAgent hrAgent() {
        ReactAgent hrAgent = ReactAgent.builder()
                .name("hr_agent")
                .model(chatModel)
                .description("处理招聘信息、投递简历、面试安排等人力资源问题")
                .instruction("你是一个专业的人力资源，擅长各种人力相关、招聘、面试、入职等业务流程。请根据用户的提问进行回答。")
                .outputKey("hr_response")
                .enableLogging(true)
                .build();
        return hrAgent;
    }

    // 可以在这个类中，给HrAgent装配一些人力相关的工具，比如查询入职计划安排、获取面试官信息、获取HR信息、组织面试等
}
