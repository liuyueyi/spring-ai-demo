package com.git.hui.springai.app.serializer;

import org.bsc.langgraph4j.serializer.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

/**
 * 普通对象的 Json 序列化
 *
 * @author YiHui
 * @date 2025/8/12
 */
public class JsonSerializer<T> implements Serializer<T> {
    private Class<T> type;

    public JsonSerializer(Class<T> type) {
        this.type = type;
    }

    @Override
    public void write(T recommendRes, ObjectOutput objectOutput) throws IOException {
        String text = JsonUtil.toStr(recommendRes);
        objectOutput.writeObject(text);
    }

    @Override
    public T read(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        String json = Objects.toString(objectInput.readObject());
        return JsonUtil.toObj(json, type);
    }
}
