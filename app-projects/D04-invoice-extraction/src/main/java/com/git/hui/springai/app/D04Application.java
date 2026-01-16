package com.git.hui.springai.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author YiHui
 * @date 2025/8/4
 */
@SpringBootApplication
public class D04Application {
    public static void main(String[] args) {
        SpringApplication.run(D04Application.class, args);
        System.out.println("启动成功，前端测试访问地址： http://localhost:8081/invoicePage");
    }
}