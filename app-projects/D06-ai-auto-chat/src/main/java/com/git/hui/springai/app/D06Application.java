package com.git.hui.springai.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * AI 自动聊天应用启动类
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Controller
@SpringBootApplication
public class D06Application {

    @RequestMapping({"/", ""})
    public String index() {
        return "simpleChat";
    }


    public static void main(String[] args) {
        SpringApplication.run(D06Application.class, args);
    }
}