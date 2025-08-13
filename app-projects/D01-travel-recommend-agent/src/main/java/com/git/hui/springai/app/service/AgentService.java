package com.git.hui.springai.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * @author YiHui
 * @date 2025/8/12
 */
@Service
public class AgentService {
    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private final ChatClient chatClient;

    public AgentService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).defaultSystem("You are now an AI assistant that provides weather, travel, and food recommendations.").defaultTools(new WeatherTools()).build();
    }

    public String weather(String input) {
        return execute("You are now an AI assistant for weather query", input);
    }

    public String travel(String input) {
        return execute("You are now an AI assistant for travel query", input);
    }

    public String food(String input) {
        return execute("You are now an AI assistant for food query", input);
    }

    public String execute(String system, String input) {
        try {
            // Validate input
            if (input == null || input.isBlank()) {
                throw new IllegalArgumentException("input is blank.");
            }

            log.info("[Call LLM] query: {} ", input);

            return chatClient.prompt()
                    .system(system)
                    .user(input)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("some exception for input = '{}': {}", input, e.getMessage(), e);
            throw new RuntimeException("error, try later!", e);
        }
    }
}
