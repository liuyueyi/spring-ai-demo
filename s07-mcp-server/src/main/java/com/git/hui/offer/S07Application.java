package com.git.hui.offer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * @author YiHui
 * @date 2025/7/11
 */
@ServletComponentScan
@SpringBootApplication
public class S07Application {
    public static void main(String[] args) {
        SpringApplication.run(S07Application.class, args);
    }
}