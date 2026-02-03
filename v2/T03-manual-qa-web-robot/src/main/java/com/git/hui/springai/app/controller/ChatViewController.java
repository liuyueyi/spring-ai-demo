package com.git.hui.springai.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author YiHui
 * @date 2026/1/28
 */
@Controller
public class ChatViewController {

    @RequestMapping({"/", "", "chat"})
    public String index() {
        return "chat";
    }

    @RequestMapping("redpacket")
    public String about() {
        return "redpacket";
    }
}
