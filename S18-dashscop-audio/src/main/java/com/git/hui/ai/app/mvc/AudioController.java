package com.git.hui.ai.app.mvc;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileOutputStream;

/**
 * @author YiHui
 * @date 2025/12/13
 */
@Slf4j
@RestController
public class AudioController {

    @Value("${spring.tts.api-key}")
    private static String apiKey;
    @Value("${spring.tts.model:qwen3-tts-flash-2025-11-27}")
    private static String ttsModel;
    @Value("${spring.tts.url:https://dashscope.aliyuncs.com/api/v1}")
    private String url;

    public String call(String text) throws Exception {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 新加坡和北京地域的API Key不同。获取API Key：https://help.aliyun.com/zh/model-studio/get-api-key
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(apiKey)
                .model(ttsModel)
                .text(text)
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese") // 建议与文本语种一致，以获得正确的发音和自然的语调。
                .build();
        MultiModalConversationResult result = conv.call(param);
        String audioUrl = result.getOutput().getAudio().getUrl();
        log.info("百炼返回结果是：{}", result);

        // 下载音频文件到本地
        byte[] audioData = downloadAudioFromUrl(audioUrl);
        try (FileOutputStream out = new FileOutputStream("downloaded_audio.wav")) {
            out.write(audioData);
        } catch (Exception e) {
            log.error("\n下载音频文件时出错: " + e.getMessage());
        }
        return audioUrl;
    }

    private byte[] downloadAudioFromUrl(String audioUrl) throws Exception {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(audioUrl)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("下载音频文件失败: " + response.code());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new RuntimeException("音频文件内容为空");
            }

            return responseBody.bytes();
        }
    }

    @GetMapping("/audio")
    public String toAudio(String text) throws Exception {
        return call(text);
    }
}
