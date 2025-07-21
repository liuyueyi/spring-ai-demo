package com.git.hui.offer.model;

import com.git.hui.offer.util.JsonUtil;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 官方文档请求参数说明 ： https://www.xfyun.cn/doc/spark/HTTP%E8%B0%83%E7%94%A8%E6%96%87%E6%A1%A3.html#_3-%E8%AF%B7%E6%B1%82%E8%AF%B4%E6%98%8E
 *
 * @author YiHui
 * @date 2025/7/21
 */
public class POJOConvert {

    public static List<Generation> generationList(SparkPOJO.ChatCompletionChunk completionChunk) {
        return completionChunk.choices().stream().map(choice -> {
            Map<String, Object> metadata = Map.of("id", completionChunk.sid(), "role", choice.message().role(), "index", choice.index(), "finishReason", completionChunk.code() == 0 ? "over" : "error");
            return buildGeneration(choice, metadata);

        }).toList();
    }

    public static Generation buildGeneration(SparkPOJO.Choice choice, Map<String, Object> metadata) {
        AssistantMessage assistantMessage = new AssistantMessage(choice.message().content(), metadata);
        ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder().finishReason((String) metadata.get("finishReason")).build();
        return new Generation(assistantMessage, generationMetadata);
    }

    public static ChatResponseMetadata from(Long reqTime, String model, SparkPOJO.ChatCompletionChunk result) {
        Assert.notNull(result, "SparkLite ChatCompletionResult must not be null");
        return ChatResponseMetadata.builder().id(result.sid() != null ? result.sid() : "").usage((Usage) (result.usage() != null ? result.usage() : new EmptyUsage())).model(model).keyValue("created", reqTime).build();
    }

    public static String toReq(Prompt prompt, String defaultModel) {
        Map<String, Object> map = new HashMap<>();
        map.put("model", (prompt.getOptions() == null || prompt.getOptions().getModel() == null) ? defaultModel : prompt.getOptions().getModel());
        map.put("stream", false);
        ChatOptions options = prompt.getOptions();
        if (options != null) {
            // 取值范围[0, 2] 默认值1.0	核采样阈值
            map.put("temperature", options.getTemperature());
            // 取值范围(0, 1] 默认值1	生成过程中核采样方法概率阈值，例如，取值为0.8时，仅保留概率加起来大于等于0.8的最可能token的最小集合作为候选集。取值越大，生成的随机性越高；取值越低，生成的确定性越高。
            map.put("top_p", options.getTopP());
            // 取值范围[1, 6] 默认值4	从k个中随机选择一个(非等概
            map.put("top_k", options.getTopK());
            // 	取值范围[-2.0,2.0] 默认0	重复词的惩罚值
            map.put("presence_penalty", options.getPresencePenalty());
            // 取值范围[-2.0,2.0] 默认0	频率惩罚值
            map.put("frequency_penalty", options.getFrequencyPenalty());
            // 最大长度
            map.put("max_tokens", options.getMaxTokens());

            // todo 等待补齐的事 function tool
        }


        List<Map> msgs = new ArrayList<>();
        for (Message message : prompt.getInstructions()) {
            Map msg = Map.of("role", message.getMessageType().getValue().toLowerCase(), "content", message.getText());
            msgs.add(msg);
        }
        map.put("messages", msgs);
        String body = JsonUtil.toStr(map);
        System.out.println("请求参数:" + body);
        return body;
    }
}
