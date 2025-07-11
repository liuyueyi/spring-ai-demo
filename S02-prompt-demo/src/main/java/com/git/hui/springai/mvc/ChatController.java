package com.git.hui.springai.mvc;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/7/11
 */
@RestController
public class ChatController {

    private final ZhiPuAiChatModel chatModel;

    @Autowired
    public ChatController(ZhiPuAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * temperature 参数作用：
     * 用于控制生成文本的随机性或创造性。
     * 数值范围一般在 0.0 到 1.0 之间（有时也可超出该范围）：
     * 当 temperature 接近 0.0 时，输出会趋于确定性和保守，通常选择概率最高的词；
     * 当 temperature 接近 1.0 或更高时，输出更具多样性和创造性，可能会选择低概率但更有趣的词。
     * 示例中设置为 0.7d，表示适度平衡确定性与多样性。
     * <p>
     * user 参数作用：
     * 用于标识请求的发起者，通常是用户的唯一标识符（如用户名、ID 等）。
     * 主要用途包括：
     * 日志记录和审计：便于追踪哪个用户触发了此次 AI 调用；
     * 配额管理：某些平台依据 user 字段进行使用量统计与限制；
     * 行为分析：用于后续的数据分析或个性化推荐等场景。
     * 示例中设置为 "一灰灰"，可能代表当前请求来源的用户身份标识。
     *
     * @param message
     * @return
     */
    @GetMapping("/ai/generate")
    public Map generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        Prompt prompt = new Prompt(message,
                ZhiPuAiChatOptions.builder()
                        .model(ZhiPuAiApi.ChatModel.GLM_4_Flash.getValue())
                        .temperature(0.7d)
                        .user("一灰灰")
                        .build()
        );

        Generation generation = chatModel.call(prompt).getResult();
        return Map.of("generation", generation == null ? "" : generation.getOutput().getText());
    }

    @GetMapping("/ai/childGenerate")
    public Map childJokeGenerate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        Prompt prompt = new Prompt(
                Arrays.asList(new SystemMessage("你现在是一个专注于给3-5岁儿童聊天的助手"), new UserMessage(message)),
                ZhiPuAiChatOptions.builder()
                        .model(ZhiPuAiApi.ChatModel.GLM_4_Flash.getValue())
                        .temperature(0.7d)
                        .user("一灰灰")
                        .build()
        );
        Generation generation = chatModel.call(prompt).getResult();

        Prompt p = new Prompt(new SystemMessage("你现在扮演一个可爱的女朋友角色，我扮演你的男朋友"), new UserMessage("你好"));
        return Map.of("generation", generation == null ? "" : generation.getOutput().getText());
    }


    @GetMapping(path = "/ai/roleChat")
    public String roleChat(@RequestParam(value = "personality", defaultValue = "温柔") String personality,
                           @RequestParam(value = "aiRole", defaultValue = "女朋友") String aiRole,
                           @RequestParam(value = "myRole", defaultValue = "男朋友") String myRole,
                           @RequestParam(value = "msg", defaultValue = "最近心情不好") String msg) {
        SystemPromptTemplate promptTemplate = new SystemPromptTemplate("我们现在开始角色扮演的聊天，你来扮演{personality}的{aiRole}, 我来扮演{myRole}");
        Message systemMsg = promptTemplate.createMessage(Map.of("personality", personality, "aiRole", aiRole, "myRole", myRole));

        Prompt prompt = new Prompt(systemMsg, new UserMessage(msg));

        Generation generation = chatModel.call(prompt).getResult();
        return generation == null ? "" : generation.getOutput().getText();
    }

    @GetMapping(path = "/ai/roleChatV2")
    public String roleChatV2(@RequestParam(value = "personality", defaultValue = "温柔") String personality,
                             @RequestParam(value = "aiRole", defaultValue = "女朋友") String aiRole,
                             @RequestParam(value = "myRole", defaultValue = "男朋友") String myRole,
                             @RequestParam(value = "msg", defaultValue = "最近心情不好") String msg) {
        PromptTemplate promptTemplate = PromptTemplate.builder().renderer(StTemplateRenderer.builder()
                        .startDelimiterToken('<').endDelimiterToken('>').build())
                .template("我们现在开始角色扮演的对话，你来扮演{personality}的{aiRole}, 我来扮演{myRole}")
                .build();
        String text = promptTemplate.render(Map.of("personality", personality, "aiRole", aiRole, "myRole", myRole));
        Prompt prompt = new Prompt(new SystemMessage(text), new UserMessage(msg));

        Generation generation = chatModel.call(prompt).getResult();
        return generation == null ? "" : generation.getOutput().getText();
    }

    @Value("classpath:/prompts/system-message.st")
    private Resource systemResource;

    @GetMapping(path = "/ai/roleChatV3")
    public String roleChatV3(@RequestParam(value = "personality", defaultValue = "温柔") String personality,
                             @RequestParam(value = "aiRole", defaultValue = "女朋友") String aiRole,
                             @RequestParam(value = "myRole", defaultValue = "男朋友") String myRole,
                             @RequestParam(value = "msg", defaultValue = "最近心情不好") String msg) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        Message text = systemPromptTemplate.createMessage(Map.of("personality", personality, "aiRole", aiRole, "myRole", myRole));
        Prompt prompt = new Prompt(text, new UserMessage(msg));

        Generation generation = chatModel.call(prompt).getResult();
        return generation == null ? "" : generation.getOutput().getText();
    }
}