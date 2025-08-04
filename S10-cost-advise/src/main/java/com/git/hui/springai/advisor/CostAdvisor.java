package com.git.hui.springai.advisor;

import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 * @author YiHui
 * @date 2025/8/4
 */
public class CostAdvisor implements CallAdvisor, StreamAdvisor {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CostAdvisor.class);

    /**
     * 同步调用
     *
     * @param chatClientRequest
     * @param callAdvisorChain
     * @return
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        long start = System.currentTimeMillis();
        // 添加上下文
        chatClientRequest.context().put("start-time", start);
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
        long end = System.currentTimeMillis();
        long cost = end - start;
        response.context().put("end-time", end);
        response.context().put("cost-time", cost);
        log.info("Prompt call cost: {} ms", cost);
        return response;
    }

    /**
     * 流式调用
     *
     * @param chatClientRequest
     * @param streamAdvisorChain
     * @return
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        long start = System.currentTimeMillis();
        chatClientRequest.context().put("start-time", start);
        Flux<ChatClientResponse> response = streamAdvisorChain.nextStream(chatClientRequest);
        return new ChatClientMessageAggregator().aggregateChatClientResponse(response, (res) -> {
            long end = System.currentTimeMillis();
            long cost = end - start;
            // 添加耗时
            res.context().put("end-time", end);
            res.context().put("cost-time", cost);
            log.info("Prompt stream cost: {} ms", cost);
        });
    }

    @Override
    public String getName() {
        return "costAdvisor";
    }

    @Override
    public int getOrder() {
        // 指定最高优先级
        return Integer.MIN_VALUE;
    }
}
