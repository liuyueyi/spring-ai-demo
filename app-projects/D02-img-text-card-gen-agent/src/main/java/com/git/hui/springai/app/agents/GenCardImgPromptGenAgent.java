package com.git.hui.springai.app.agents;

import com.git.hui.springai.app.executor.TxtImgAgentState;
import com.git.hui.springai.app.model.CardInfo;
import com.git.hui.springai.app.model.TxtImgCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于关键词，给出文生图的提示词
 *
 * @author YiHui
 * @date 2025/8/15
 */
@Service
public class GenCardImgPromptGenAgent {
    private static final Logger log = LoggerFactory.getLogger(GenCardImgPromptGenAgent.class);
    public static final String AGENT_NAME = "gen-img-prompt";

    private final ChatClient chatClient;

    public GenCardImgPromptGenAgent(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你现在是一个专业的儿童绘画师，善于绘制童真、卡通、温馨可爱的风格的图片，擅长会根据用户输入的提示信息，生成一张适合5-10岁小孩的文生图的提示词
                        希望生成的图片的背景为空白或者纯色，画面风格以重点突出关键事物为主
                        """)
                .build();

    }

    public Map<String, Object> apply(TxtImgAgentState state) {
        List<CardInfo> list = state.getCardInfo();
        log.info("[GenCardImgPromptGenAgent] req: {}", list);

        List<TxtImgCard> res = new ArrayList<>();
        for (CardInfo card : list) {
            String url = genImgPrompt(card);
            res.add(new TxtImgCard(url, "", card));
        }
        Map<String, Object> map = Map.of(TxtImgAgentState.GEN_IMG_PROMPT, res);
        log.info("[GenCardImgPromptGenAgent] res: {}", map);
        return map;
    }

    private String genImgPrompt(CardInfo card) {
        return this.chatClient.prompt(new PromptTemplate("关键词：{word}, 说明：{desc}").render(Map.of("word", card.zh(), "desc", card.zhDesc())))
                .call()
                .content();
    }
}
