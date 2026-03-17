package com.git.hui.springai.ali.writer;

import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 *
 * @author YiHui
 * @date 2026/3/17
 */
@Component
public class SeqAgent {
    public static final String SEQ_AGENT = "seq_agent";

    @Autowired
    private ReviewAgent reviewAgent;
    @Autowired
    private WriterAgent writerAgent;
    @Autowired
    private OutlineAgent outlineAgent;

    public SequentialAgent seqAgent() {
        SequentialAgent agent = SequentialAgent.builder()
                .name(SEQ_AGENT)
                .description("根据用户给定的主题写一篇文章：先写大纲，然后再协作，最后进行评审和润色")
                .subAgents(List.of(outlineAgent.outlineAgent(), writerAgent.writerAgent(), reviewAgent.reviewAgent()))
                .build();
        return agent;
    }
}
