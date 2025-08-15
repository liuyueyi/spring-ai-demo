package com.git.hui.springai.app.executor;

import com.git.hui.springai.app.model.CardInfo;
import com.git.hui.springai.app.model.CardReq;
import com.git.hui.springai.app.model.TxtImgCard;
import com.git.hui.springai.app.serializer.JsonSerializer;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.bsc.langgraph4j.spring.ai.serializer.std.MessageSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/8/15
 */
public class TxtImgAgentState extends AgentState {
    public static final String INPUT_TEXT = "input";

    public static final String GEN_CARD_INFO = "cardInfo";

    public static final String GEN_IMG_PROMPT = "imgPrompt";

    public static final String CARD_RESULT = "cards";

    public TxtImgAgentState(Map<String, Object> initData) {
        super(initData);
    }

    public CardReq getInput() {
        return (CardReq) value(INPUT_TEXT).orElse(new CardReq("", 1));
    }

    public List<CardInfo> getCardInfo() {
        return (List<CardInfo>) value(GEN_CARD_INFO).orElse(new ArrayList<>());
    }

    public List<TxtImgCard> getImgCards() {
        return (List<TxtImgCard>) value(GEN_IMG_PROMPT).orElse(new ArrayList<>());
    }

    /**
     * 提供序列化方式，默认使用ObjectStreamStateSerializer，无法有效支持Java POJO类的序列化
     *
     * @return An instance of `StateSerializer` for serializing and deserializing `State` objects.
     */
    public static StateSerializer<TxtImgAgentState> serializer() {
        var serializer = new ObjectStreamStateSerializer<>(TxtImgAgentState::new);
        serializer.mapper().register(Message.class, new MessageSerializer());
        serializer.mapper().register(CardReq.class, new JsonSerializer<>(CardReq.class));
        serializer.mapper().register(CardInfo.class, new JsonSerializer<>(CardInfo.class));
        serializer.mapper().register(TxtImgCard.class, new JsonSerializer<>(TxtImgCard.class));
        return serializer;
    }
}
