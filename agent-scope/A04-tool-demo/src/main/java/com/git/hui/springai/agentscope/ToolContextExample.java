package com.git.hui.springai.agentscope;

import com.git.hui.springai.EnvUtil;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;

import java.util.List;

/**
 *
 * @author YiHui
 * @date 2026/7/23
 */
public class ToolContextExample {
    /**
     * Runs the RuntimeContext injection example.
     *
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new PersonalizedTools());

        // 使用推理模型（支持原生思维链）
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(EnvUtil.getSiliconApiKey())
                .baseUrl("https://api.siliconflow.cn")
                .endpointPath("/v1/chat/completions")
                .modelName("Qwen/Qwen3-8B")
                .stream(true)                    // ← 流式输出开关
                .formatter(new OpenAIChatFormatter())
                .build();

        ReActAgent agent =
                ReActAgent.builder()
                        .name("PersonalizedAgent")
                        .sysPrompt(
                                "You are a personalized assistant. "
                                        + "Use the greet and preferences tools.")
                        .model(model)
                        .toolkit(toolkit)
                        .build();

        // 调用时传入上下文
        RuntimeContext ctx = RuntimeContext.builder()
                .put(UserContext.class, new UserContext("一灰灰", "zh"))
                .userId("yihuihui")
                .build();
        var response = agent.call(List.of(new UserMessage("问候我")), ctx).block();
        System.out.println(response.getTextContent());
    }

    // 1. 定义业务上下文类（普通 POJO）
    public record UserContext(String username, String locale) {
    }

    // 2. 定义工具，无 @ToolParam 的参数会被框架自动注入
    public static class PersonalizedTools {

        @Tool(name = "greet", description = "用自定义问候语问候用户")
        public String greet(
                @ToolParam(name = "greeting", description = "问候语，例如 '你好'")
                String greeting,              // 由 LLM 提供
                UserContext userCtx) {                 // 由框架自动注入
            return greeting + "，" + (userCtx == null ? "未知用户" : userCtx.username()) + "！";
        }
    }
}
