package com.git.hui.springai.advance.mvc;

import com.git.hui.springai.advance.mem.MemAgent;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.spring.ai.agentexecutor.AgentExecutor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Content;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author YiHui
 * @date 2025/8/5
 */
@RestController
public class ChatController {
    private final CompiledGraph<AgentExecutor.State> workflow;

    public ChatController(ChatModel chatModel) throws GraphStateException {
        this.workflow = new MemAgent(chatModel).workflow();
    }

    /**
     * 聊天对话
     *
     * @param user
     * @param msg
     * @return
     */
    @GetMapping("/{user}/chat")
    public Object chat(@PathVariable String user, String msg) {
        var runnableConfig = RunnableConfig.builder().threadId(user).build();
        var state = workflow.invoke(Map.of("messages", new UserMessage(msg)), runnableConfig).orElseThrow();

        return state.lastMessage().map((Content::getText)).orElse("No Response");
    }
}
