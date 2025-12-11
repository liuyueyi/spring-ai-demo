package com.git.hui.ai.app.mvc;

import io.micrometer.common.util.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/8/26
 */
@RestController
public class ChatController {
    /**
     * 阿里的百炼模型
     */
    private final ChatModel dashModel;


    /**
     * 智谱模型
     */
    private final ChatModel zhipuModel;

    public ChatController(Environment environment) {
        // 通过手动的方式，注册 阿里百炼模型
        OpenAiApi openAiApi = OpenAiApi.builder().apiKey(getApiKey(environment, "dash-api-key"))
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                .completionsPath("/v1/chat/completions")
                .build();
        dashModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen-plus-latest")
                        .extraBody(Map.of("enable_thinking", true))
                        .build())
                .build();

        OpenAiApi zhipuApi = OpenAiApi.builder().apiKey(getApiKey(environment, "zhipuai-api-key"))
                .baseUrl("https://open.bigmodel.cn")
                .completionsPath("/api/paas/v4/chat/completions")
                .build();
        zhipuModel = OpenAiChatModel.builder()
                .openAiApi(zhipuApi)
                .defaultOptions(OpenAiChatOptions.builder().model("glm-4.5-flash")
//                        支持思考推理（前提是大模型本身要支持这个能力）
//                        默认是开启推理，可以使用下面的方式关闭
//                        .extraBody(Map.of("thinking", Map.of("type", "disabled")))
                        .build())
                .build();

    }

    private String getApiKey(Environment environment, String key) {
        // 1. 通过 --dash-api-key 启动命令传参
        String val = environment.getProperty(key);
        if (StringUtils.isBlank(val)) {
            // 2. 通过jvm传参 -Ddash-api-key=
            val = System.getProperty(key);
            if (val == null) {
                // 3. 通过环境变量传参
                val = System.getenv(key);
            }
        }
        return val;
    }

    /**
     * 阿里百炼模型
     *
     * @param msg
     * @return
     */
    @GetMapping(path = "aliChatWithThinking", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatV5(String msg) {
        SseEmitter sseEmitter = new SseEmitter();
        Flux<ChatResponse> res = dashModel.stream(new Prompt(new UserMessage(msg)));
        StringBuilder content = new StringBuilder();
        StringBuilder reason = new StringBuilder();
        res.doOnComplete(() -> {
                    sseEmitter.complete();
                    System.out.println("思考过程:" + reason);
                    System.out.println("结果:" + content);
                })
                .subscribe(txt -> {
                    Generation generation = txt.getResult();

                    var r = generation.getOutput().getMetadata().get("reasoningContent");
                    if (r != null) {
                        reason.append(r);
                    }

                    var t = generation.getOutput().getText();
                    if (t != null) {
                        content.append(t);
                    }
                    try {
                        sseEmitter.send("思考:" + reason + "===>\n<br/>\n==>" + content);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        return sseEmitter;
    }

    /**
     * 智谱模型
     *
     * @param msg
     * @return
     */
    @GetMapping(path = "zhipuChat")
    public Map zhipuChat(String msg) {
        ChatClient client = ChatClient.builder(zhipuModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();
        Flux<ChatResponse> res = client.prompt(new Prompt(msg)).stream().chatResponse();
        StringBuilder content = new StringBuilder();
        StringBuilder reason = new StringBuilder();
        ChatResponse response = res.doOnComplete(() -> {
            System.out.println("思考过程:" + reason);
            System.out.println("结果:" + content);
        }).doOnNext(txt -> {
            Generation generation = txt.getResult();
            var r = generation.getOutput().getMetadata().get("reasoningContent");
            if (r != null) {
                reason.append(r);
                System.out.println("思考:" + r);
            }
            var t = generation.getOutput().getText();
            if (t != null) {
                content.append(t);
                System.out.println("结果:" + t);
            }
        }).blockLast();
        // token使用
        var usage = response.getMetadata().getUsage();
        // 构建完成的返回结果
        return Map.of("思考过程", reason, "结果", content, "token消耗", usage);
    }

    @GetMapping(path = "zhipuChatV2")
    public Map zhipuChatV2(String msg) {
        ChatClient client = ChatClient.builder(zhipuModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        ChatResponse response = client.prompt(new Prompt(msg)).call().chatResponse();
        var reason = response.getResult().getOutput().getMetadata().get("reasoningContent");
        var content = response.getResult().getOutput().getText();

        // token使用
        var usage = response.getMetadata().getUsage();
        // 构建完成的返回结果
        return Map.of("思考过程", reason == null ? "" : reason, "结果", content, "token消耗", usage);
    }
}
