package com.git.hui.springai.ali;

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
public class L02Application {

    @RequestMapping({"", "/", "sql-chat"})
    public String sql() {
        return "sql-chat";
    }

    public static void main(String[] args) {
        SpringApplication.run(L02Application.class, args);
    }

}