package com.git.hui.offer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/7/21
 */
public interface SparkPOJO {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true
    )
    record ChatCompletionChunk(
            // 错误码 0 成功
            Integer code,
            // 错误码的描述信息
            String message,
            // 本次请求的唯一id
            String sid,
            // 大模型返回结果
            List<Choice> choices,
            // 本次请求的消耗信息
            Usage usage) {
    }

    record Choice(Integer index, SparkMsg message) {
    }

    record SparkMsg(String role, String content) {
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(
            ignoreUnknown = true
    )
    record Usage(Integer completionTokens, Integer promptTokens,
                 Integer totalTokens) implements org.springframework.ai.chat.metadata.Usage {
        public Usage(@JsonProperty("completion_tokens") Integer completionTokens, @JsonProperty("prompt_tokens") Integer promptTokens, @JsonProperty("total_tokens") Integer totalTokens) {
            this.completionTokens = completionTokens;
            this.promptTokens = promptTokens;
            this.totalTokens = totalTokens;
        }

        @JsonProperty("completion_tokens")
        public Integer completionTokens() {
            return this.completionTokens;
        }

        @JsonProperty("prompt_tokens")
        public Integer promptTokens() {
            return this.promptTokens;
        }

        @JsonProperty("total_tokens")
        public Integer totalTokens() {
            return this.totalTokens;
        }

        @Override
        public Integer getPromptTokens() {
            return promptTokens;
        }

        @Override
        public Integer getCompletionTokens() {
            return completionTokens;
        }

        @Override
        public Object getNativeUsage() {
            Map<String, Integer> usage = new HashMap<>();
            usage.put("promptTokens", this.promptTokens());
            usage.put("completionTokens", this.completionTokens());
            usage.put("totalTokens", this.totalTokens());
            return usage;
        }
    }
}
