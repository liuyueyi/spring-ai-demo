package com.git.hui.springai.app.mvc;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/8/5
 */
@Controller
public class ChatController {
    private final ChatClient chatClient;

    @Autowired
    private List<McpAsyncClient> mcpClients;

    public ChatController(ChatModel chatModel, ToolCallbackProvider toolCallbackProvider) {
        System.out.println("当前注册的工具数量: " + toolCallbackProvider.getToolCallbacks().length);
        this.chatClient = ChatClient.builder(chatModel)
                // 将mcp client 作为大模型的工具来使用
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build(),
                        new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * 首页
     *
     * @param model
     * @return
     */
    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }


    /**
     * 直接调用mcp服务
     *
     * @param area
     * @return
     */
    @GetMapping("/directCallMcp")
    @ResponseBody
    public Object directCallMcp(String area) {
        var tools = mcpClients.get(0).listTools().block().tools();
        System.out.println("当前mcp的工具: " + tools);


        Mono<McpSchema.CallToolResult> result = mcpClients.get(0).callTool(
                new McpSchema.CallToolRequest("getTimeByZoneId", Map.of("area", area))
        );
        return result.block().content();
    }

    /**
     * 用户问答
     *
     * @param message
     * @param model
     * @return
     */
    @PostMapping("/ask")
    public HtmxResponse chat(String message, Model model) {
        String res = this.chatClient.prompt(message).call().content();
        model.addAttribute("question", message);
        model.addAttribute("response", res);
        // 返回 chat.html 中 chatFragment 经过渲染后的内容，用于对话的填充
        return HtmxResponse.builder().view("chat :: chatFragment").build();
    }
}
