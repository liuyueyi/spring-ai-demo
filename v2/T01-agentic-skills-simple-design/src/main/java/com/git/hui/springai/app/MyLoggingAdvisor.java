package com.git.hui.springai.app;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author YiHui
 * @date 2026/1/26
 */
public class MyLoggingAdvisor implements BaseAdvisor {
    private final int order;

    public final boolean showSystemMessage;

    public final boolean showAvailableTools;

    private AtomicInteger cnt = new AtomicInteger(1);

    private MyLoggingAdvisor(int order, boolean showSystemMessage, boolean showAvailableTools) {
        this.order = order;
        this.showSystemMessage = showSystemMessage;
        this.showAvailableTools = showAvailableTools;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        System.out.println("======================= 第 " + cnt.getAndAdd(1) + " 轮 ====================================");

        StringBuilder sb = new StringBuilder("\nUSER: ");

        if (this.showSystemMessage && chatClientRequest.prompt().getSystemMessage() != null) {
            sb.append("\n - SYSTEM: ").append(first(chatClientRequest.prompt().getSystemMessage().getText(), 300));
        }

        if (this.showAvailableTools) {
            Object tools = "No Tools";

            if (chatClientRequest.prompt().getOptions() instanceof ToolCallingChatOptions toolOptions) {
                tools = toolOptions.getToolCallbacks().stream().map(tc -> tc.getToolDefinition().name()).toList();
            }

            sb.append("\n - TOOLS: ").append(ModelOptionsUtils.toJsonString(tools));
        }

        Message lastMessage = chatClientRequest.prompt().getLastUserOrToolResponseMessage();

        if (lastMessage.getMessageType() == MessageType.TOOL) {
            ToolResponseMessage toolResponseMessage = (ToolResponseMessage) lastMessage;
            for (var toolResponse : toolResponseMessage.getResponses()) {
                var tr = toolResponse.name() + ": " + first(toolResponse.responseData(), 1000);
                sb.append("\n - TOOL-RESPONSE: ").append(tr);
            }
        } else if (lastMessage.getMessageType() == MessageType.USER) {
            if (StringUtils.hasText(lastMessage.getText())) {
                sb.append("\n - TEXT: ").append(first(lastMessage.getText(), 1000));
            }
        }

        System.out.println("before: " + sb);
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        StringBuilder sb = new StringBuilder("\nASSISTANT: ");

        if (chatClientResponse.chatResponse() == null || chatClientResponse.chatResponse().getResults() == null) {
            sb.append(" No chat response ");
            System.out.println("after: " + sb);
            return chatClientResponse;
        }

        for (var generation : chatClientResponse.chatResponse().getResults()) {
            var message = generation.getOutput();
            if (message.getToolCalls() != null) {
                for (var toolCall : message.getToolCalls()) {
                    sb.append("\n - TOOL-CALL: ")
                            .append(toolCall.name())
                            .append(" (")
                            .append(toolCall.arguments())
                            .append(")");
                }
            }

            if (message.getText() != null) {
                if (StringUtils.hasText(message.getText())) {
                    sb.append("\n - TEXT: ").append(first(message.getText(), 1200));
                }
            }
        }

        System.out.println("after: " + sb);
        return chatClientResponse;
    }

    private String first(String text, int n) {
        if (text.length() <= n) {
            return text;
        }
        return text.substring(0, n) + "...";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int order = 0;

        private boolean showSystemMessage = true;

        private boolean showAvailableTools = true;

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder showSystemMessage(boolean showSystemMessage) {
            this.showSystemMessage = showSystemMessage;
            return this;
        }

        public Builder showAvailableTools(boolean showAvailableTools) {
            this.showAvailableTools = showAvailableTools;
            return this;
        }

        public MyLoggingAdvisor build() {
            MyLoggingAdvisor advisor = new MyLoggingAdvisor(this.order, this.showSystemMessage,
                    this.showAvailableTools);
            return advisor;
        }
    }

}
