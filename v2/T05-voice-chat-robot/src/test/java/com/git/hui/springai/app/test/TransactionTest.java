package com.git.hui.springai.app.test;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author YiHui
 * @date 2026/2/27
 */
public class TransactionTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionTest.class);

    private static final String apiKey = "";

    @Test
    public ResponseEntity<String> createTranscription() throws IOException {
        // 1. 直接使用 ClassPathResource（更简洁）
        ClassPathResource audioResource = new ClassPathResource("test.mp3");
        if (!audioResource.exists()) {
            throw new IllegalArgumentException("音频文件不存在: test.mp3");
        }

        // 2. 验证文件大小
        if (audioResource.contentLength() == 0) {
            throw new IllegalArgumentException("音频文件为空");
        }

        log.info("音频文件大小: {} bytes", audioResource.contentLength());


        RestClient restClient = RestClient.builder()
                // 直接使用 RestClient 方式进行交互，始终报错，主要原因就是 RestClient 的底层 Client 不是 JDK HttpClient，而是某个不支持 multipart 的实现,从而导致音频上传无法被正确解析
                .requestFactory(new JdkClientHttpRequestFactory())
                .baseUrl("https://api.siliconflow.cn/v1").build();

        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        // 文件部分
        builder.part("file", audioResource).filename("test.mp3").contentType(MediaType.parseMediaType("audio/mpeg"));
        // 普通文本字段
        builder.part("model", "FunAudioLLM/SenseVoiceSmall");

        MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

        return restClient
                .post()
                .uri("/audio/transcriptions")
                .header("Authorization", "Bearer " + apiKey)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipartBody)
                .retrieve()
                .toEntity(String.class);
    }


    // 使用Apache HttpClient可以正常处理
    @Test

    public ResponseEntity<String> upload() {
        try {
            ClassPathResource audioResource = new ClassPathResource("test.mp3");
            if (!audioResource.exists()) {
                throw new IllegalArgumentException("音频文件不存在: test.mp3");
            }

            // 1. 验证文件存在性和内容
            byte[] fileBytes = audioResource.getContentAsByteArray();
            if (fileBytes.length == 0) {
                throw new IllegalArgumentException("音频文件为空");
            }

            log.info("文件验证通过 - 大小: {} bytes", fileBytes.length);

            // 2. 使用最原始的方式构造请求进行调试
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("https://api.siliconflow.cn/v1/audio/transcriptions");

            // 设置所有必要的头部
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Accept", "application/json");

            // 3. 构造精确的 multipart 实体
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();  // 使用浏览器兼容模式

            // 添加文件字段
            builder.addBinaryBody(
                    "file",
                    fileBytes,
                    ContentType.create("audio/mpeg"),
                    "test.mp3"
            );

            // 添加模型字段
            builder.addTextBody("model", "TeleAI/TeleSpeechASR",
                    ContentType.TEXT_PLAIN.withCharset("UTF-8"));

            org.apache.http.HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);

            // 4. 启用详细的请求日志
            log.info("准备发送请求...");
            log.info("URL: {}", httpPost.getURI());
            log.info("Method: {}", httpPost.getMethod());
            log.info("Headers: {}", Arrays.toString(httpPost.getAllHeaders()));

            // 5. 执行请求
            CloseableHttpResponse response = httpClient.execute(httpPost);

            // 6. 详细记录响应
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            log.info("=== 请求详情 ===");
            log.info("状态码: {}", statusCode);
            log.info("响应头: {}", Arrays.toString(response.getAllHeaders()));
            log.info("响应体: {}", responseBody);
            log.info("=============== ");

            // 7. 返回响应
            return ResponseEntity.status(statusCode).body(responseBody);

        } catch (Exception e) {
            log.error("请求执行失败", e);
            return ResponseEntity.status(500).body("请求失败: " + e.getMessage());
        }
    }
}
