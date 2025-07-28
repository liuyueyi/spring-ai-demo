package com.git.hui.offer.service;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author YiHui
 * @date 2025/7/27
 */
@Configuration
public class ToolConfig {
    @Bean
    public ToolCallbackProvider dateProvider(DateService dateService) {
        return MethodToolCallbackProvider.builder().toolObjects(dateService).build();
    }
}
