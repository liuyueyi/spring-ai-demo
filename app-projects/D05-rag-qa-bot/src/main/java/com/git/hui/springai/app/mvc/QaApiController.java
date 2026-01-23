package com.git.hui.springai.app.mvc;

import com.git.hui.springai.app.qa.QaBoltService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.Collection;

/**
 * API控制器，专门处理SSE流请求
 * @author YiHui
 * @date 2026/1/21
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class QaApiController {
    @Autowired
    private QaBoltService qaBolt;

    /**
     * 问答对话 - GET版本（用于非文件上传场景）
     *
     * @param chatId
     * @param question
     * @return
     */
    @GetMapping(path = "/chat/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> qaGet(@PathVariable("chatId") String chatId,
                              @RequestParam("question") String question) {
        // 对于GET请求，没有文件上传，所以传递空集合
        return qaBolt.ask(chatId, question, java.util.Collections.emptyList());
    }

    /**
     * 问答对话 - POST版本（用于文件上传场景）
     *
     * @param chatId
     * @param question
     * @param files
     * @return
     */
    @PostMapping(path = "/chat/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> qaPost(@PathVariable("chatId") String chatId,
                               @RequestParam("question") String question,
                               @RequestParam(value = "files", required = false) Collection<MultipartFile> files) {
        if (files == null) {
            files = java.util.Collections.emptyList();
            log.info("no files uploaded");
        }
        return qaBolt.ask(chatId, question, files);
    }
}