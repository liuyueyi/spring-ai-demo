package com.git.hui.springai.mvc;

import io.micrometer.common.util.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author YiHui
 * @date 2025/8/26
 */
@RestController
public class ChatController {

    private final ChatClient chatClient;

    /**
     * 阿里的百炼模型
     */
    private final ChatModel dashModel;


    public ChatController(ChatModel chatModel, Environment environment) {
        chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();


        // 通过手动的方式，注册 阿里百炼模型
        OpenAiApi openAiApi = OpenAiApi.builder().apiKey(getDashApiKey(environment))
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                .build();
        dashModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model("qwen-plus").build())
                .build();

    }

    private String getDashApiKey(Environment environment) {
        final String key = "dash-api-key";
        // 1. 通过 --dash-api-key 启动命令传参
        String val = environment.getProperty(key);
        if (StringUtils.isBlank(val)) {
            // 2. 通过jvm传参 -Ddash-api-key=
            val = System.getProperty(key);
            if (val == null) {
                // 3. 通过环境变量传参
                val = System.getenv(key);
            }
        }
        return val;
    }

    @GetMapping(path = "chat")
    public String chat(String msg) {
        return chatClient.prompt(msg).call().content();
    }


    /**
     * 阿里百炼模型
     *
     * @param msg
     * @return
     */
    @GetMapping(path = "aliChat")
    public String aliChat(String msg) {
        return dashModel.call(new UserMessage(msg));
    }
}
