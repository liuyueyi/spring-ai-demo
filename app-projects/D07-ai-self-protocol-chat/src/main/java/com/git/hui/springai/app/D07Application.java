package com.git.hui.springai.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author YiHui
 * @date 2026/3/6
 */
@Controller
@SpringBootApplication
public class D07Application {

    @RequestMapping({"/", ""})
    public String index() {
        return "index";
    }


    public static void main(String[] args) {
        SpringApplication.run(D07Application.class, args);
    }
}