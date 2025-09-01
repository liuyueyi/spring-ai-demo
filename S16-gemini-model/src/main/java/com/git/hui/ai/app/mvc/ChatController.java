package com.git.hui.ai.app.mvc;

import io.micrometer.common.util.StringUtils;
import io.netty.channel.ChannelOption;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.env.Environment;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;


/**
 * @author YiHui
 * @date 2025/8/26
 */
@RestController
public class ChatController {

    private final ChatModel genimiModel;

    public ChatController(Environment environment) {
        // 通过手动的方式，注册 谷歌 Gemini 模型
        OpenAiApi openAiApi = createOpenAiApiWithProxy(getApiKey(environment, "gemini-ap-key"),
                "https://generativelanguage.googleapis.com",
                "/v1beta/openai/chat/completions",
                "127.0.0.1", 10809);
        genimiModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model("gemini-2.5-flash").build())
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

    private OpenAiApi createOpenAiApiWithProxy(String apiKey, String baseUrl, String path, String proxyHost, int proxyPort) {
        // 创建代理配置
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .proxy(type -> type
                        .type(ProxyProvider.Proxy.HTTP)
                        .host(proxyHost)
                        .port(proxyPort));

        // 创建 OpenAiApi
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .completionsPath(path)
                .webClientBuilder(WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)))
                .build();
    }

    @GetMapping(path = "geminiChat")
    public String geminiChat(String msg) {
        return genimiModel.call(Prompt.builder().content(msg).build()).getResult().getOutput().getText();
    }

}
