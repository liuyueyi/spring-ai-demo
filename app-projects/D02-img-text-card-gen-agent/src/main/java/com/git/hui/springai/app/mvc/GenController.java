package com.git.hui.springai.app.mvc;

import com.git.hui.springai.app.executor.AgentExecutor;
import com.git.hui.springai.app.executor.TxtImgAgentState;
import com.git.hui.springai.app.model.CardReq;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author YiHui
 * @date 2025/8/15
 */
@Controller
public class GenController {
    private AgentExecutor agentExecutor;

    public GenController(AgentExecutor agentExecutor) {
        this.agentExecutor = agentExecutor;
    }

    @GetMapping("/gen")
    @ResponseBody
    public Object gen(String msg, @RequestParam(name = "size", defaultValue = "1") Integer size) {
        TxtImgAgentState state = agentExecutor.invoke(new CardReq(msg, size));
        return state;
    }

    @GetMapping("/card")
    public String cards(String msg, @RequestParam(name = "size", defaultValue = "1") Integer size, Model model) {
        TxtImgAgentState state = agentExecutor.invoke(new CardReq(msg, size));

        model.addAttribute("cards", state.getImgCards());
        return "template_card";
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("cards", List.of());
        return "template_card";
    }
}
