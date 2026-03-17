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
public class ReviewAgent {
    private static final String PROMPT = """
            你是一个资深的文章评审专家，负责对文章进行润色和优化。
            请审阅以下文章，修正语法错误、优化表达、确保逻辑清晰，请始终以幽默风趣、并结合实际用例进行辅助说明的语气进行扩展
            最终只返回修改后的文章，不要包含评论。
            """;
    @Autowired
    private ChatModel chatModel;

    public ReactAgent reviewAgent() {
        ReactAgent agent = ReactAgent.builder()
                .name("review_agent")
                .model(chatModel)
                .description("专业评审Agent")
                .systemPrompt(PROMPT)
                // 用于在当前 Agent 节点处插入新的问题说明，引导模型和流程运行。支持使用占位符（如 {input}、{outputKey} 等）来动态引用状态中的数据，实现 Agent 之间的数据传递。
                .instruction("用户输入：{input} \nWriteAgent生成的待审阅的文章初稿:\n {article_draft}")
                .outputKey("final_article")
                // 父流程中可能包含非常多子 Agent 的推理过程、每个子 Agent 的输出等。
                // includeContents 用来控制当前子 Agent 执行时，是只基于自己的 instruction 给到的内容工作，还是会带上所有父流程的上下文。
                // 设置为 false 可以让子 Agent 专注于自己的任务，不受父流程复杂上下文的影响。
                .includeContents(false)
                // 控制子 Agent 的上下文是否返回父流程中。
                // 如果设置为 false，则其他 Agent 不会有机会看到这个子 Agent 内部的推理过程，它们只能看到这个 Agent 输出的内容（比如通过 outputKey 引用）。
                // 这对于减少上下文大小、提高效率非常有用。
                .returnReasoningContents(false)
                .enableLogging(true)
                .build();
        return agent;
    }
}
