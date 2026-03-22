package com.git.hui.springai.ali;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author YiHui
 * @date 2026/3/6
 */
@Slf4j
@Controller
@SpringBootApplication
public class L07Application {

    @RequestMapping(path = {"", "/", "index", "cs-chat"})
    public String csChat() {
        return "cs-chat";
    }

    public static void main(String[] args) {
        SpringApplication.run(L07Application.class, args);
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> applicationReadyEventListener(Environment environment) {
        return event -> {
            String port = environment.getProperty("server.port", "8080");
            String contextPath = environment.getProperty("server.servlet.context-path", "");
            String accessUrl = "http://localhost:" + port + contextPath;
            System.out.println("\n🎉========================================🎉");
            System.out.println("✅ L07-multi-agent-route example is ready!");
            System.out.println("🚀 Chat with agents: " + accessUrl);
            System.out.println("🎉========================================🎉\n");
        };
    }
}