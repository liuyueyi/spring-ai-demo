package com.git.hui.offer.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author YiHui
 * @date 2025/7/21
 */
public class JsonUtil {
    public static String toStr(Object prompt) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(prompt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromStr(String str, Class<T> clazz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(str, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
