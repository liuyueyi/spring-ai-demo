package com.git.hui.springai.mvc;

import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 图片识别
 *
 * @author YiHui
 * @date 2025/8/4
 */
@RestController
public class ImgRecognitionController {

    private final ChatClient chatClient;

    public ImgRecognitionController(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }


    /**
     * 图片识别
     *
     * @param imgUrl
     * @return
     */
    @RequestMapping(path = "recognition")
    public String recognition(@RequestParam(name = "imgUrl") String imgUrl,
                              @RequestParam(name = "msg") String msg) {
        // 根据传入的图片地址，获取图片内容，然后由大模型进行图片识别
        byte[] imgs = HttpUtil.downloadBytes(imgUrl);

        String text = new PromptTemplate("{msg}, 请将图片内容进行识别，并返回结果").render(Map.of("msg", msg));
        Media media = Media.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .data(imgs)
                .build();
        Message userMsg = UserMessage.builder().text(text).media(media).build();
        Prompt prompt = new Prompt(userMsg);
        return chatClient.prompt(prompt).call().content();
    }

    @RequestMapping(path = "recognitionAndOutput")
    public FoodDetail recognitionAndOutput(@RequestParam(name = "imgUrl") String imgUrl,
                                           @RequestParam(name = "msg") String msg) {
        // 根据传入的图片地址，获取图片内容，然后由大模型进行图片识别
        byte[] imgs = HttpUtil.downloadBytes(imgUrl);

        String text = new PromptTemplate("{msg}, 请将图片内容进行识别，并返回结果").render(Map.of("msg", msg));
        Media media = Media.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .data(imgs)
                .build();
        Message userMsg = UserMessage.builder().text(text).media(media).build();
        Prompt prompt = new Prompt(userMsg);
        return chatClient.prompt(prompt).call().entity(FoodDetail.class);
    }

    public record FoodDetail(
            @JsonPropertyDescription("整张图片的描述")
            String desc,
            @JsonPropertyDescription("总的卡路里")
            Double totalCalorie,
            @JsonPropertyDescription("卡路里计算方式说明")
            String calorieDesc,
            @JsonPropertyDescription("图片中的食材列表")
            List<FoodItem> itemList) {
    }

    public record FoodItem(
            @JsonPropertyDescription("食材名")
            String food,
            @JsonPropertyDescription("食材的卡路里占用描述")
            String desc,
            @JsonPropertyDescription("食材数量")
            Integer cnt,
            @JsonPropertyDescription("最小的卡路里含量")
            Double minCalorie,
            @JsonPropertyDescription("最大的卡路里含量")
            Double maxCalorie) {
    }
}
