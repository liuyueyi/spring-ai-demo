package com.git.hui.springai.mvc;

import com.git.hui.springai.advisor.CostAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author YiHui
 * @date 2025/7/14
 */
@RestController
public class ChatController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;

    public ChatController(ChatModel chatModel) {
        chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你现在扮演盛唐的著名诗人李白，我们开始对话")
                .defaultAdvisors(new CostAdvisor())
                .build();
    }


    @GetMapping("/call")
    public String call(String msg) {
        ChatClientResponse response = chatClient.prompt(msg).call().chatClientResponse();
        log.info("上下文：{}", response.context());
        return response.chatResponse().getResult().getOutput().getText();
    }

    @GetMapping(path = "/stream", produces = "text/event-stream")
    public Flux<String> stream(String msg) {
        Flux<String> ans = chatClient.prompt(msg).stream().content();
        return ans;
    }
}
