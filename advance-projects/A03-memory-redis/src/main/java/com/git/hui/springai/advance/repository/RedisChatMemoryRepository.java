package com.git.hui.springai.advance.repository;

import com.git.hui.springai.advance.util.JsonUtil;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 使用redis来存储用户聊天上下文
 * <p>
 * 方案一：定义list的数据结构，key 为 conversationId, value 为消息信息
 * 方案二：定义hash的数据结构，field 为 conversationId, value 为列表
 *
 * @author YiHui
 * @date 2025/8/7
 */
@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String PREFIX = "chat:";

    /**
     * 查询所有会话id
     *
     * @return
     */
    @Override
    public List<String> findConversationIds() {
        Set<String> ans = redisTemplate.keys(PREFIX + "*");
        return ans.stream().map(key -> key.substring(PREFIX.length())).collect(Collectors.toList());
    }

    /**
     * 查询会话记录
     *
     * @param conversationId 会话id
     * @return
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = PREFIX + conversationId;
        // 查询对话历史
        List<String> ans = redisTemplate.opsForList().range(key, 0, -1);
        if (CollectionUtils.isEmpty(ans)) {
            return Collections.emptyList();
        }

        return ans.stream().map(item -> JsonUtil.toObj(item, Message.class)).collect(Collectors.toList());
    }

    /**
     * 保存会话记录
     *
     * @param conversationId 会话id
     * @param messages       当前上下文的全量数据
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = PREFIX + conversationId;
        List<String> messageJsons = messages.stream().map(JsonUtil::toStr).toList();
        // 先删除旧数据
        redisTemplate.delete(key);
        // 添加新数据，采用覆盖式更新方式
        redisTemplate.opsForList().leftPushAll(key, messageJsons);
    }

    /**
     * 删除会话记录
     *
     * @param conversationId
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(PREFIX + conversationId);
    }
}
