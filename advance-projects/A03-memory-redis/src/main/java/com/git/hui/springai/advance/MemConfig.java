package com.git.hui.springai.advance;

import com.git.hui.springai.advance.repository.RedisChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author YiHui
 * @date 2025/8/6
 */
@Configuration
public class MemConfig {
    @Bean
    public ChatMemory jdbcChatMemory(RedisChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .build();
    }
}
