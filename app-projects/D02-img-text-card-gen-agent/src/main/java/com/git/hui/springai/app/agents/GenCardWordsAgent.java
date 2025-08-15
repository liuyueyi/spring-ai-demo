package com.git.hui.springai.app.agents;

import com.git.hui.springai.app.executor.TxtImgAgentState;
import com.git.hui.springai.app.model.CardInfo;
import com.git.hui.springai.app.model.CardReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生成卡片文字
 *
 * @author YiHui
 * @date 2025/8/15
 */
@Service
public class GenCardWordsAgent {
    private static final Logger log = LoggerFactory.getLogger(GenCardWordsAgent.class);
    public static final String AGENT_NAME = "gen-card-words";

    private final ChatModel chatModel;

    private BeanOutputConverter<List<CardInfo>> listConverter;
    private BeanOutputConverter<CardInfo> converter;

    public GenCardWordsAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
        listConverter = new BeanOutputConverter<>(new ParameterizedTypeReference<List<CardInfo>>() {
        });
        converter = new BeanOutputConverter<>(CardInfo.class);
    }

    public Map<String, Object> apply(TxtImgAgentState state) {
        CardReq input = state.getInput();
        log.info("[GenCardWordsAgent] input: {}", input);

        Message systemMessage = SystemPromptTemplate.builder()
                .template("""
                        你现在是一个资深的儿童教育专家，擅长从给定文本中，分析出儿童感兴趣的事物，要求最多只返回{size}个关键词，以列表的方式返回。比如
                         用户输入：“我希望给小朋友介绍一下桃子”
                         提取关键事物：桃子
                         """)
                .build()
                .createMessage(Map.of("size", input.size()));


        Prompt prompt = new Prompt(systemMessage, new UserMessage(PromptTemplate.builder().template("""
                {text}.
                {format}                     
                """).build().render(Map.of("text", input.text(), "format", listConverter.getFormat()))));

        Generation generation = chatModel.call(prompt).getResult();
        String out = generation.getOutput().getText();
        List<CardInfo> list = convert(out);

        Map<String, Object> res = new HashMap<>();
        res.put(TxtImgAgentState.GEN_CARD_INFO, list);

        log.info("[GenCardWordsAgent] output: {}", res);
        return res;
    }


    public List<CardInfo> convert(String source) {
        try {
            return listConverter.convert(source);
        } catch (Exception e) {
            // 如果只返回了一条，为了避免解析异常，这里做一个兼容
            CardInfo card = converter.convert(source);
            return List.of(card);
        }
    }
}
