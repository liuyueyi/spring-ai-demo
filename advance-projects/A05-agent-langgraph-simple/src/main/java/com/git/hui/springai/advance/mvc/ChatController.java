package com.git.hui.springai.advance.mvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.git.hui.springai.advance.times.TimeTools;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.spring.ai.agentexecutor.AgentExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Content;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author YiHui
 * @date 2025/8/5
 */
@RestController
public class ChatController {
    private final CompiledGraph<AgentExecutor.State> workflow;

    private final ChatClient chatClient;

    public ChatController(ChatModel chatModel) throws GraphStateException {
        workflow = AgentExecutor.builder()
                .chatModel(chatModel)
                .toolsFromObject(new TimeTools())
                .build()
                .compile();

        chatClient = ChatClient.builder(chatModel)
                .defaultTools(new TimeTools())
                .build();
    }

    public String toStr(Object obj) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 通过agent方式访问大模型
     *
     * @param msg
     * @return
     */
    @GetMapping("/chat")
    public Object chat(String msg) {
        AgentExecutor.State last = null;
        int i = 0;
        for (NodeOutput<AgentExecutor.State> item : workflow.stream(Map.of("messages", new UserMessage(msg)))) {
            System.out.println(item);
            last = item.state();
            System.out.printf("%02d : %s%n", i++, toStr(last.messages()));
        }

        // 返回最后一条消息
        return last.lastMessage().map(Content::getText).orElse("NoData");
    }

    /**
     * 直接调用大模型
     *
     * @param msg
     * @return
     */
    @GetMapping("/chat2")
    public Object chat2(String msg) {
        return chatClient.prompt(msg).call().content();
    }
}
