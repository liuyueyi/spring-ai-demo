package com.git.hui.springai;

import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.io.IOException;

/**
 * 基于大模型的语音识别
 *
 * @author YiHui
 * @date 2026/2/25
 */
@RestController
public class AudioTransactionController {
    private Logger log = org.slf4j.LoggerFactory.getLogger(AudioTransactionController.class);
    private TranscriptionModel initTranscriptionModel;

    /**
     * 直接使用默认注册的模型对象来进行交互
     */
    @Autowired
    private TranscriptionModel transcriptionModel;

    public AudioTransactionController(Environment environment) {
        OpenAiAudioApi openAiAudioApi = OpenAiAudioApi.builder()
                .apiKey(getApiKey(environment, "silicon-api-key"))
                .baseUrl("https://api.siliconflow.cn")
                .build();

        initTranscriptionModel = new OpenAiAudioTranscriptionModel(openAiAudioApi,
                OpenAiAudioTranscriptionOptions.builder().model("TeleAI/TeleSpeechASR").build());
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

    @Value("classpath:/test.mp3")
    private Resource resource;

    /**
     * 使用OpenAI风格的语言识别
     *
     * @return
     * @throws IOException
     */
    @GetMapping(path = "translateAudio")
    @ResponseBody
    public Object translateAudio() throws IOException {
        AudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                .model("TeleAI/TeleSpeechASR")
                .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                .build();
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(resource, options);
        AudioTranscriptionResponse response = initTranscriptionModel.call(prompt);
        log.info("response -> {}", response.getResult());
        return response.getResult().getOutput();
    }

    @GetMapping(path = "translateAudioV2")
    @ResponseBody
    public Object translateAudioV2() throws IOException {
        AudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.JSON)
                .model("FunAudioLLM/SenseVoiceSmall")
                .language("zh")
                .build();
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(resource, options);
        AudioTranscriptionResponse response = transcriptionModel.call(prompt);
        log.info("response -> {}", response.getResult());
        return response.getResult().getOutput();
    }
}
