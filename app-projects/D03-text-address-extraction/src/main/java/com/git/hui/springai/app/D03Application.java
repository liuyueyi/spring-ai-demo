package com.git.hui.springai.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author YiHui
 * @date 2025/7/11
 */
@SpringBootApplication
public class D03Application {
    public static void main(String[] args) {
        SpringApplication.run(D03Application.class, args);

        System.out.println("启动成功，前端测试访问地址： http://localhost:8080/addressPage");
    }
}