package com.git.hui.springai.app.controller;

import com.git.hui.springai.app.service.AudioTransactionService;
import io.micrometer.common.util.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音实时翻译的对话实现
 *
 * @author YiHui
 * @date 2026/2/27
 */
@RestController
@RequestMapping(path = "auto")
public class AutoTranslateController {
    @Autowired
    private AudioTransactionService audioTransactionService;

    private final ChatClient chatClient;

    // 存储任务状态的内存映射
    private final Map<String, TranslationTask> taskMap = new ConcurrentHashMap<>();
    private Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final String TRANS_SYSTEM_PROMPT = """
            你是一名专业的翻译专家，精通多种语言。请根据以下要求完成翻译任务：
            - 保持原文的语气、风格和情感色彩（如正式/口语、文学/技术等）；
            - 确保翻译准确、通顺，符合目标语言表达习惯；
            - 如果遇到文化特定词汇或歧义，请根据上下文合理意译，并在必要时添加简短注释（可选）；
            - 直接输出翻译结果，无需额外解释。
                            
            请将以下文本翻译成 {lan}：
                        
            {content}
            """;

    // 翻译任务记录类
    private record TranslationTask(String taskId, MultipartFile file, String targetLanguage) {
    }

    public AutoTranslateController(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }


    /**
     * 音频转录并翻译（同步版本）
     *
     * @param file 音频文件
     * @param targetLanguage 目标语言
     * @param useParallel 是否使用并行处理
     * @return 翻译结果
     * @throws IOException
     */
    @PostMapping(path = "autoTranslateSync")
    @ResponseBody
    public String autoTranslateSync(
            @RequestParam("file") MultipartFile file, 
            @RequestParam(required = false) String targetLanguage,
            @RequestParam(defaultValue = "false") boolean useParallel) throws IOException {
        
        if (StringUtils.isBlank(targetLanguage)) {
            targetLanguage = "英语";
        }

        // 使用并行或单线程处理
        String ans = audioTransactionService.audioTransactionParallel(file, useParallel);
        System.out.println("转录结果：" + ans);

        PromptTemplate promptTemplate = new PromptTemplate(TRANS_SYSTEM_PROMPT);
        Prompt prompt = promptTemplate.create(Map.of("lan", targetLanguage, "content", ans));
        return chatClient.prompt(prompt).call().content();
    }

    /**
     * 第一步：上传音频文件并返回任务ID
     *
     * @param file           音频文件
     * @param targetLanguage 目标语言
     * @return 任务ID
     * @throws IOException
     */
    @PostMapping(path = "uploadAudio")
    @ResponseBody
    public Map<String, String> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String targetLanguage) throws IOException {

        if (StringUtils.isBlank(targetLanguage)) {
            targetLanguage = "英语";
        }

        // 生成唯一任务ID
        String taskId = UUID.randomUUID().toString();

        // 创建翻译任务
        TranslationTask task = new TranslationTask(taskId, file, targetLanguage);
        taskMap.put(taskId, task);

        // 异步处理任务
        processTranslationTask(taskId);

        // 返回任务ID
        return Map.of("taskId", taskId);
    }


    /**
     * 第二步：基于任务ID建立SSE连接获取处理结果
     *
     * @param taskId 任务ID
     * @return SSEEmitter
     */
    @GetMapping(path = "getResultStream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getResultStream(@PathVariable String taskId) {
        SseEmitter emitter = new SseEmitter();
        emitters.put(taskId, emitter);
        return emitter;
    }

    /**
     * 异步处理翻译任务
     *
     * @param taskId 任务ID
     */
    private void processTranslationTask(String taskId) {
        TranslationTask task = taskMap.get(taskId);
        if (task == null) return;


        new Thread(() -> {
            SseEmitter sseEmitter = null;
            try {
                // 1. 音频转录
                String transcription = audioTransactionService.audioTransaction(task.file());
                System.out.println("音频转录结果：" + transcription);
                while (true) {
                    sseEmitter = emitters.get(taskId);
                    if (sseEmitter != null) {
                        break;
                    }

                    Thread.sleep(10);
                }
                sseEmitter.send(SseEmitter.event().name("transcription").data(transcription));

                // 2. 翻译处理
                PromptTemplate promptTemplate = new PromptTemplate(TRANS_SYSTEM_PROMPT);
                Prompt prompt = promptTemplate.create(Map.of(
                        "lan", task.targetLanguage,
                        "content", transcription));
                // 流式获取翻译结果
                SseEmitter finalSseEmitter = sseEmitter;

                Flux<String> res = chatClient.prompt(prompt).stream().content();
                res.doOnComplete(() -> {
                            try {
                                finalSseEmitter.send(SseEmitter.event().name("end").data("true"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            finalSseEmitter.complete();
                            taskMap.remove(taskId);
                            emitters.remove(taskId);
                        })
                        .subscribe(txt -> {
                            System.out.println("翻译结果：" + txt);
                            try {
                                finalSseEmitter.send(SseEmitter.event().name("translation").data(txt));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            } catch (Exception e) {
                System.err.println("处理任务出错：" + e.getMessage());
                if (sseEmitter != null) {
                    sseEmitter.completeWithError(e);
                }
                taskMap.remove(taskId);
                emitters.remove(taskId);
            }
        }).start();
    }

    @GetMapping(path = "translate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> translate(String text, String targetLanguage) {
        PromptTemplate promptTemplate = new PromptTemplate(TRANS_SYSTEM_PROMPT);
        Prompt prompt = promptTemplate.create(Map.of(
                "lan", targetLanguage,
                "content", text));
        return chatClient.prompt(prompt).stream().content();
    }

}
