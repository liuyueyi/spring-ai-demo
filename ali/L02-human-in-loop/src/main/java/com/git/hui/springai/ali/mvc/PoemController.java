package com.git.hui.springai.ali.mvc;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author YiHui
 * @date 2026/3/9
 */
@RestController
public class PoemController {

    @Autowired
    private ChatModel chatModel;

    @RequestMapping(path = "/poem")
    public Object poemCreator(String msg) throws Exception {
        MemorySaver memorySaver = new MemorySaver();

        ToolCallback poetTool = FunctionToolCallback.builder("poem", (args) -> "春江潮水连海平，海上明月共潮生...")
                .description("写诗工具")
                .inputType(Map.class)
                .build();

        HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
                .approvalOn("poem", ToolConfig.builder()
                        .description("请确认诗歌创作操作")
                        .build())
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("poet_agent")
                .model(chatModel)
                .tools(List.of(poetTool))
                .hooks(List.of(humanInTheLoopHook))
                .saver(memorySaver)
                .build();

        String threadId = "user-session-001";
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 第一次调用 - 触发中断
        System.out.println("=== 第一次调用：期望中断 ===");
        Optional<NodeOutput> result = agent.invokeAndGetOutput(msg, config);

        // 检查中断并处理
        if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
            InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

            System.out.println("检测到中断，需要人工审批");
            List<InterruptionMetadata.ToolFeedback> toolFeedbacks = interruptionMetadata.toolFeedbacks();

            for (InterruptionMetadata.ToolFeedback feedback : toolFeedbacks) {
                System.out.println("工具: " + feedback.getName());
                System.out.println("参数: " + feedback.getArguments());
                System.out.println("描述: " + feedback.getDescription());
            }

            System.out.println("模拟批准反馈，对每个工具都设置为批准执行~");

            // 构建批准反馈
            InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
                    .nodeId(interruptionMetadata.node())
                    .state(interruptionMetadata.state());

            // 对每个工具调用设置批准决策
            interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
                InterruptionMetadata.ToolFeedback approvedFeedback =
                        InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                                .result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
                                .build();
                feedbackBuilder.addToolFeedback(approvedFeedback);
            });

            InterruptionMetadata approvalMetadata = feedbackBuilder.build();

            // 使用批准决策恢复执行
            RunnableConfig resumeConfig = RunnableConfig.builder()
                    .threadId(threadId) // 相同的线程ID以恢复暂停的对话
                    .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, approvalMetadata)
                    .build();

            // 第二次调用以恢复执行
            System.out.println("\n=== 第二次调用：使用批准决策恢复 ===");
            Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);

            if (finalResult.isPresent()) {
                System.out.println("最终结果: " + finalResult.get());
                System.out.println("执行完成");
                return finalResult.get().state().data().get("messages");
            }
        }

        return "over";
    }
}
