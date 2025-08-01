package com.git.hui.springai.mvc;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author YiHui
 * @date 2025/7/14
 */
@RestController
public class ChatController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;

    private final ChatClient poemClient;

    public ChatController(ChatModel chatModel) {
        chatClient = ChatClient.builder(chatModel).build();

        poemClient = ChatClient.builder(chatModel)
                .defaultSystem("你现在扮演著名的诗人{role}，接下来我们进行对话")
                .defaultOptions(ChatOptions.builder().maxTokens(500).build())
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
        Poem poem = chatClient.prompt()
                .system("你现在扮演盛唐著名的诗人李白，接下来我们进行对话")
                .user(msg)
                .call().entity(Poem.class);
        return poem;
    }

    @GetMapping("/ai/batchGen")
    public Object batchGen(@RequestParam(value = "msg", defaultValue = "你好") String msg) {
        List<Poem> poem = chatClient.prompt()
                .system("你现在扮演盛唐著名的诗人李白，接下来我们进行对话")
                .user(msg)
                .call().entity(new ParameterizedTypeReference<List<Poem>>() {
                });
        return poem;
    }

    @GetMapping(path = "/ai/fluxGen", produces = "text/event-stream")
    public Flux<String> fluxGen(@RequestParam(value = "msg", defaultValue = "你好") String msg) {
        return chatClient.prompt()
                .system("你现在扮演盛唐著名的诗人李白，接下来我们进行对话")
                .user(msg).stream().content();
    }

    @GetMapping(path = "/ai/fluxGenV2")
    public List<Poem> fluxGenV2(@RequestParam(value = "msg", defaultValue = "你好") String msg) {
        var converter = new BeanOutputConverter<>(new ParameterizedTypeReference<List<Poem>>() {
        });

        Flux<String> flux = chatClient.prompt()
                .system("你现在扮演盛唐著名的诗人李白，接下来我们进行对话")
                .user(u -> u.text("{msg}.\n{format}").param("msg", msg).param("format", converter.getFormat()))
                .stream().content();
        String content = flux.collectList().block().stream().collect(Collectors.joining());
        return converter.convert(content);
    }


    /**
     * 提示词模板
     *
     * @param role
     * @param msg
     * @return
     */
    @GetMapping("/ai/template")
    public String template(@RequestParam(value = "role", defaultValue = "李白") String role,
                           @RequestParam(value = "msg", defaultValue = "你好") String msg) {
        return chatClient.prompt()
                .system(u -> u.text("你现在扮演盛唐著名的诗人{role}，接下来我们进行对话")
                        .param("role", role))
                .user(u -> u.text("我是一个现代诗歌爱好者，我的提问是：{msg}").params(Map.of("msg", msg)))
//                使用自定义的模板变量替换规则
//                .templateRenderer(StTemplateRenderer.builder().startDelimiterToken('{').endDelimiterToken('}').build())
                .call().content();
    }

    @GetMapping("/ai/poet")
    public Poem poetChat(String role, String msg) {
        return poemClient.prompt().system(sp -> sp.param("role", role))
                .user(msg)
                .call().entity(Poem.class);
    }


    @Autowired
    private ChatMemory chatMemory;

    @GetMapping("/ai/historyChat")
    public String historyChat(String msg) {
        return chatClient.prompt()
                .system("你现在扮演盛唐著名诗人李白，我们接下来开启对话")
                .user(msg)
                .advisors(new SimpleLoggerAdvisor(
                                req -> ("[request] " + req),
                                res -> ("[response] " + res),
                                0),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .call().content();
    }


    record Poem(String title, String content) {
    }

}
