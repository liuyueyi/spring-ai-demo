package com.git.hui.springai.app.protocol.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author YiHui
 * @date 2026/3/6
 */
public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T toObj(String str, Class<T> t) {
        try {
            return objectMapper.readValue(str, t);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
