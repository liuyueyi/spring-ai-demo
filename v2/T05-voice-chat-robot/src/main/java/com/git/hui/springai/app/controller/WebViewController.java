package com.git.hui.springai.app.controller;

import com.git.hui.springai.app.service.AudioTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author YiHui
 * @date 2026/2/27
 */
@Controller
public class WebViewController {
    @Autowired
    private AudioTransactionService audioTransactionService;

    @GetMapping(path = "up")
    public String up() {
        return "up";
    }


    @GetMapping(path = "translate")
    public String translate() {
        return "translate";
    }


    /**
     * 音频转录为文字（单线程版本）
     *
     * @param file 音频文件
     * @return 转录结果
     * @throws IOException
     */
    @PostMapping(path = "translateAudio")
    @ResponseBody
    public String translateAudio(@RequestParam("file") MultipartFile file) throws IOException {
        return audioTransactionService.audioTransaction(file);
    }

    /**
     * 音频转录为文字（并行处理版本）
     *
     * @param file        音频文件
     * @param useParallel 是否使用并行处理
     * @return 转录结果
     * @throws IOException
     */
    @PostMapping(path = "translateAudioParallel")
    @ResponseBody
    public String translateAudioParallel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean useParallel) throws IOException {
        return audioTransactionService.audioTransactionParallel(file, useParallel);
    }
}
