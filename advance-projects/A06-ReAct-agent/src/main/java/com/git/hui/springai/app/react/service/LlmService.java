package com.git.hui.springai.app.react.service;

import com.git.hui.springai.app.advisor.MyLoggingAdvisor;
import io.micrometer.common.util.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author YiHui
 * @date 2026/3/3
 */
@Service
public class LlmService {
    @Autowired
    private ChatModel chatModel;

    private Map<String, ChatClient> chatClientMap = new ConcurrentHashMap<>();

    public ChatClient getChatClient(String modelName) {
        if (StringUtils.isBlank(modelName)) {
            modelName = "Qwen/Qwen2.5-7B-Instruct";
        }

        if (chatClientMap.containsKey(modelName)) {
            return chatClientMap.get(modelName);
        }

        ChatClient client = ChatClient.builder(chatModel).defaultOptions(ChatOptions.builder().model(modelName).build())
                .defaultAdvisors(
                        // Custom logging advisor
                        MyLoggingAdvisor.builder()
                                .showAvailableTools(true)
                                .showSystemMessage(true)
                                .build())
                .build();
        chatClientMap.put(modelName, client);
        return client;
    }

}
