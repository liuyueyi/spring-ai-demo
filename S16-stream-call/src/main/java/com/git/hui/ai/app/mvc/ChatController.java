package com.git.hui.ai.app.mvc;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;


/**
 * @author YiHui
 * @date 2025/12/11
 */
@RestController
public class ChatController {

    private final ChatModel chatModel;

    private final ChatClient chatClient;

    public ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 流式访问, sse 的方式返回文本给用户
     *
     * @param msg
     * @return
     */
    @GetMapping(path = "chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chatV1(String msg) {
        return this.chatModel.stream(new Prompt(msg));
    }

    @GetMapping(path = "chatV2")
    public String chatV2(String msg) {
        Flux<ChatResponse> res = chatModel.stream(new Prompt(msg));
        List<ChatResponse> responses = res.collectList().block();
        StringBuilder content = new StringBuilder();
        for (ChatResponse response : responses) {
            content.append(response.getResult().getOutput().getText());
        }
        return content.toString();
    }

    @GetMapping(path = "chatV3", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatV3(String msg) {
        return chatClient.prompt(msg).stream().content();
    }

    @GetMapping(path = "chatV4")
    public String chatV4(String msg) {
        Flux<String> res = chatClient.prompt(msg).stream().content();
        String content = res.reduce("", (a, b) -> a + b).block();
//        res .collect(StringBuilder::new, StringBuilder::append).block().toString();
        return content;
    }

    @GetMapping(path = "chatV5", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatV5(String msg) {
        SseEmitter sseEmitter = new SseEmitter();
        Flux<String> res = chatClient.prompt(msg).stream().content();
        res.doOnComplete(sseEmitter::complete)
                .subscribe(txt -> {
                    try {
                        sseEmitter.send(txt);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        return sseEmitter;
    }
}
