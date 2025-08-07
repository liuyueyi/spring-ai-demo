package com.git.hui.springai.advance.util;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * message序列化
 *
 * @author YiHui
 * @date 2025/8/7
 */
public class MessageDeserializer extends JsonDeserializer<Message> {
    private static final Logger logger = LoggerFactory.getLogger(MessageDeserializer.class);

    private final Map<String, Function<String, Message>> msgFactor = Map.of(
            "USER", UserMessage::new,
            "SYSTEM", UserMessage::new,
            "ASSISTANT", UserMessage::new
    );

    @Override
    public Message deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode node = p.getCodec().readTree(p);
        // If node is plain text, create a UserMessage by default
        if (node.isTextual()) {
            return new UserMessage(node.asText());
        }

        // Extract message type
        String type = extractMessageType(node);

        // Extract content
        String content = extractContent(node);

        // Create corresponding message object based on type
        return Optional.ofNullable(type).map(String::toUpperCase).map(msgFactor::get).orElseGet(() -> {
            if (type == null) {
                logger.warn("Message type not found, defaulting to USER");
            } else {
                logger.warn("Unknown message type: {}, defaulting to USER", type);
            }
            return msgFactor.get("USER");
        }).apply(content);
    }

    /**
     * 获取消息类型
     */
    private String extractMessageType(JsonNode node) {
        return Optional.ofNullable(node.get("messageType"))
                .map(JsonNode::asText)
                .orElse(null);
    }

    /**
     * 获取消息内容
     */
    private String extractContent(JsonNode node) {
        return Optional.ofNullable(node.get("text"))
                .map(JsonNode::asText)
                .orElseGet(node::toString);
    }
}
