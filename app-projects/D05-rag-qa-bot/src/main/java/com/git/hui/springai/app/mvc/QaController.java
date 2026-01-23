package com.git.hui.springai.app.mvc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author YiHui
 * @date 2026/1/21
 */
@Slf4j
@Controller
public class QaController {
    /**
     * 跳转到聊天页面
     *
     * @return
     */
    @GetMapping({"/", "/chat"})
    public String chatPage() {
        return "chat"; // 返回 chat.html 模板
    }

}