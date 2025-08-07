package com.git.hui.springai.advance.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.ai.chat.messages.Message;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author YiHui
 * @date 2025/7/14
 */
public class JsonUtil {
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.findAndRegisterModules();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Message.class, new MessageDeserializer());
        mapper.registerModule(module);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    }

    /**
     * 对象转字符串
     *
     * @param o 对象
     * @return 字符串
     */
    public static String toStr(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static <T> T toObj(String s, Class<T> clazz) {
        try {
            return mapper.readValue(s, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
