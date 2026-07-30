package com.git.hui.springai.agentscope;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.subagent.WorkspaceMode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 *
 * @author YiHui
 * @date 2026/7/30
 */
public class SubAgents {

    private static Model model() {
        return OpenAIChatModel.builder()
                .apiKey(EnvUtil.getZhipuApiKey())
                .baseUrl("https://open.bigmodel.cn/api/paas")
                .endpointPath("/v4/chat/completions")
                .modelName("GLM-4-Flash")
                .stream(true)
                .formatter(new OpenAIChatFormatter())
                .build();
    }

    public static void main(String[] args) {
        // 使用 Builder 模式构建 HarnessAgent
        HarnessAgent agent = HarnessAgent.builder()
                .name("assistant")                                  // Agent 名称
                .sysPrompt("你是一个有用的 AI 助手，名叫一灰灰。")    // 系统提示词 / 人设
                .model(model())
                .workspace(Paths.get(".agentscope/workspace"))      // 工作区路径
                .subagent(SubagentDeclaration.builder()
                        .name("reviewer")
                        .description("代码审查专家")
                        .workspace(Path.of("./defs/reviewer"))
                        .workspaceMode(WorkspaceMode.ISOLATED)
                        .model("glm-4-flash")
                        .steps(8)
                        .tools(List.of("read_file", "grep_files"))
                        .build())
                .build();

        // 构建运行时上下文（userId + sessionId）
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId("SubAgent-Reviewer")
                .userId("yihuihui")
                .build();

        // 执行主 Agent，传入任务指令
        String task = """
                请审查一下当前项目中的 SubAgents.java 文件，看看有没有可以优化的地方
                """;
        String result = agent.call(task, ctx).block().getTextContent();
        // 输出执行结果
        System.out.println("Agent 执行结果: \n" + result);

        var ans = agent.call("请查看文件内容，并给我优化建议", ctx).block().getTextContent();
        System.out.println("Agent 执行结果: \n" + ans);
    }

}
