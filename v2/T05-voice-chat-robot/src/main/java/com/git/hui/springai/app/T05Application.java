package com.git.hui.springai.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class T05Application {

    public static void main(String[] args) {
        SpringApplication.run(T05Application.class, args);
        System.out.println("启动成功，前端测试访问地址： http://localhost:8080/translate");
    }


    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                // 底层 Client 不是 JDK HttpClient，而是某个不支持 multipart 的实现,从而导致音频上传无法被正确解析
                .requestFactory(new JdkClientHttpRequestFactory())
                .messageConverters(converters -> {
                    converters.add(new FormHttpMessageConverter());
                    converters.add(new ResourceHttpMessageConverter());
                    converters.add(new StringHttpMessageConverter());
                });
    }
}