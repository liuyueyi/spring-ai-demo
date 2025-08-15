package com.git.hui.springai.app.agents;

import com.git.hui.springai.app.executor.TxtImgAgentState;
import com.git.hui.springai.app.model.TxtImgCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 生成卡片文字
 *
 * @author YiHui
 * @date 2025/8/15
 */
@Service
public class GenCardImgAgent {
    private final static Logger log = LoggerFactory.getLogger(GenCardImgAgent.class);
    public static final String AGENT_NAME = "gen-card-img";

    private final ImageModel imageModel;

    public GenCardImgAgent(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    public Map<String, Object> apply(TxtImgAgentState state) {
        List<TxtImgCard> list = state.getImgCards();
        log.info("[GenCardImgAgent] req: {}", list);

        List<TxtImgCard> res = new ArrayList<>();
        for (TxtImgCard card : list) {
            res.add(new TxtImgCard(card.prompt(), genImg(card), card.info()));
        }
        Map<String, Object> map = Map.of(TxtImgAgentState.GEN_IMG_PROMPT, res);
        log.info("[GenCardImgAgent] res: {}", res);
        return map;
    }

    private String genImg(TxtImgCard card) {
        ImagePrompt imgPrompt = new ImagePrompt("卡通手绘风格，简约纯色背景\n" + card.prompt(),
                ImageOptionsBuilder.builder()
                        .height(1024)
                        .width(1024)
                        .model("CogView-3-Flash")
                        // 返回图片类型
                        .responseFormat("png")
                        // 图像风格，如 vivid 生动风格， natural 自然风格
                        .style("natural")
                        .build());

        ImageGeneration response = this.imageModel.call(imgPrompt).getResult();
        return response.getOutput().getUrl();
    }
}
