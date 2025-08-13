package com.git.hui.springai.app.mvc;

import com.git.hui.springai.app.executor.MAgentExecutor;
import com.git.hui.springai.app.model.SimpleResVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author YiHui
 * @date 2025/8/12
 */
@RestController
public class TravelController {

    private final MAgentExecutor mAgentExecutor;

    public TravelController(MAgentExecutor mAgentExecutor) {
        this.mAgentExecutor = mAgentExecutor;
    }


    @GetMapping("/recommend")
    public SimpleResVo recommend(String area) {
        var result = mAgentExecutor.invoke(area);
        return result.getBlog();
    }
}
