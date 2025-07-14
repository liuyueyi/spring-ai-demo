package com.git.hui.springai.mvc;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author YiHui
 * @date 2025/7/14
 */
@RestController
public class ChatController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    private final ZhiPuAiChatModel chatModel;

    private final ChatMemory chatMemory;

    private final ChatClient chatClient;

    private final ChatClient sessionClient;

    private final ChatClient promptClient;

    @Autowired
    public ChatController(ZhiPuAiChatModel chatModel, ChatMemory chatMemory) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你现在是狂放不羁的诗仙李白，我们现在开始对话")
                .defaultAdvisors(new SimpleLoggerAdvisor(ModelOptionsUtils::toJsonStringPrettyPrinter, ModelOptionsUtils::toJsonStringPrettyPrinter, 0),
                        // 每次交互时从记忆库检索历史消息，并将其作为消息集合注入提示词
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        // 带参数的默认系统消息
        this.sessionClient = ChatClient.builder(chatModel)
                .defaultSystem("你现在是{role}，我们现在开始对话")
                .defaultAdvisors(new SimpleLoggerAdvisor(ModelOptionsUtils::toJsonStringPrettyPrinter, ModelOptionsUtils::toJsonStringPrettyPrinter, 0),
                        // 每次交互时从记忆库检索历史消息，并将其作为消息集合注入提示词
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();


        this.promptClient = ChatClient.builder(chatModel)
                .defaultSystem("你现在是狂放不羁的诗仙李白，我们现在开始对话")
                .defaultAdvisors(new SimpleLoggerAdvisor(ModelOptionsUtils::toJsonStringPrettyPrinter, ModelOptionsUtils::toJsonStringPrettyPrinter, 0),
                        // 每次交互时从记忆库检索历史消息，并将其作为消息集合注入提示词
                        PromptChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * 基于ChatClient实现返回结果的结构化映射
     *
     * @param msg
     * @return
     */
    @GetMapping("/ai/generate")
    public Object generate(@RequestParam(value = "msg", defaultValue = "你好") String msg) {
        return chatClient.prompt(msg).call().content();
    }

    @GetMapping("/ai/gen3")
    public Object gen3(@RequestParam(value = "msg", defaultValue = "你好") String msg) {
        return promptClient.prompt(msg).call().content();
    }

    @GetMapping("/ai/{user}/gen")
    public Object gen2(
            @PathVariable("user") String user,
            @RequestParam(value = "role", defaultValue = "狂放不羁的诗仙李白") String role,
            @RequestParam(value = "msg", defaultValue = "你好") String msg) {
        return sessionClient.prompt()
                // 系统词模板
                .system(sp -> sp.param("role", role))
                .user(msg)
                // 设置会话ID，实现单独会话
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, user))
                .call()
                .content();
    }
}
