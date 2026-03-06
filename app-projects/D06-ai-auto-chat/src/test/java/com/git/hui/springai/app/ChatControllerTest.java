package com.git.hui.springai.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.git.hui.springai.app.dto.ChatRequest;
import com.git.hui.springai.app.dto.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 聊天接口测试
 *
 * @author YiHui
 * @date 2026/3/5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class ChatControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 测试普通文本聊天
     */
    @Test
    public void testTextChat() {
        ChatRequest request = new ChatRequest();
        request.setMessage("你好，请介绍一下你自己");
        request.setResponseType("text");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatResponse> response = restTemplate.postForEntity("/api/chat", entity, ChatResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo("text");
        assertThat(response.getBody().getContent()).isNotEmpty();
        assertThat(response.getBody().getConversationId()).isNotNull();
    }

    /**
     * 测试卡片类型响应
     */
    @Test
    public void testCardChat() {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐一道川菜，返回卡片格式");
        request.setResponseType("card");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatResponse> response = restTemplate.postForEntity("/api/chat", entity, ChatResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo("card");
        
        // 验证返回了结构化数据
        Object data = response.getBody().getData();
        assertThat(data).isNotNull();
    }

    /**
     * 测试列表类型响应
     */
    @Test
    public void testListChat() {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐 3 本好看的科幻小说，返回列表格式");
        request.setResponseType("list");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatResponse> response = restTemplate.postForEntity("/api/chat", entity, ChatResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo("list");
        
        // 验证返回的是列表
        Object data = response.getBody().getData();
        assertThat(data).isInstanceOf(java.util.List.class);
    }

    /**
     * 测试选项类型响应
     */
    @Test
    public void testOptionsChat() {
        ChatRequest request = new ChatRequest();
        request.setMessage("我想学习编程，应该从哪种语言开始？给我几个选项");
        request.setResponseType("options");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatResponse> response = restTemplate.postForEntity("/api/chat", entity, ChatResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo("options");
        
        // 验证返回的是选项列表
        Object data = response.getBody().getData();
        assertThat(data).isInstanceOf(java.util.List.class);
    }

    /**
     * 测试多轮对话
     */
    @Test
    public void testMultiTurnChat() throws Exception {
        String conversationId = null;

        // 第一轮对话
        ChatRequest request1 = new ChatRequest();
        request1.setMessage("我叫小明，今年 18 岁");
        request1.setResponseType("text");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChatRequest> entity1 = new HttpEntity<>(request1, headers);

        ResponseEntity<ChatResponse> response1 = restTemplate.postForEntity("/api/chat", entity1, ChatResponse.class);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        conversationId = response1.getBody().getConversationId();
        System.out.println("第一轮对话 ID: " + conversationId);

        // 等待一下，确保上下文已保存
        Thread.sleep(1000);

        // 第二轮对话，测试是否记得之前的信息
        ChatRequest request2 = new ChatRequest();
        request2.setMessage("我刚才说了什么？");
        request2.setResponseType("text");
        request2.setConversationId(conversationId);

        HttpEntity<ChatRequest> entity2 = new HttpEntity<>(request2, headers);
        ResponseEntity<ChatResponse> response2 = restTemplate.postForEntity("/api/chat", entity2, ChatResponse.class);
        
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getBody().getContent()).containsIgnoringCase("小明");
    }

    /**
     * 测试 GET 方式的流式接口
     */
    @Test
    public void testGetStreamChat() {
        String url = "/api/chat/stream?message=你好&responseType=text";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // GET 方式会返回 SSE 流，这里只验证能正常响应
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
