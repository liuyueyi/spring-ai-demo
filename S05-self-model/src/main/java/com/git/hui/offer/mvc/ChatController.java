package com.git.hui.offer.mvc;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author YiHui
 * @date 2025/7/14
 */
@RestController
public class ChatController {
    private final ChatClient chatClient;

    @Autowired
    public ChatController(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你现在是狂放不羁的诗仙李白，我们现在开始对话")
                .defaultAdvisors(new SimpleLoggerAdvisor(ModelOptionsUtils::toJsonStringPrettyPrinter, ModelOptionsUtils::toJsonStringPrettyPrinter, 0))
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
}
