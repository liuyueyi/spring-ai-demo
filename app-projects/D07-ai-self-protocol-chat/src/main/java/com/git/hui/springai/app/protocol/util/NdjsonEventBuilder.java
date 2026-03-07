package com.git.hui.springai.app.protocol.util;

import com.git.hui.springai.app.protocol.dto.NdjsonEvent;
import com.git.hui.springai.app.protocol.tool.BarChartCard;
import com.git.hui.springai.app.protocol.tool.QuizCard;
import com.git.hui.springai.app.protocol.tool.WeatherCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NdjsonEvent 事件构建工具类
 * <p>
 * 封装 NDJSON 事件的创建逻辑，简化外部调用
 * </p>
 *
 * @author YiHui
 * @date 2026/3/6
 */
public class NdjsonEventBuilder {
    private static final Logger log = LoggerFactory.getLogger(NdjsonEventBuilder.class);

    // 序列号生成器
    private static final AtomicInteger sequenceGenerator = new AtomicInteger(0);

    /**
     * 私有构造函数，防止实例化
     */
    private NdjsonEventBuilder() {
    }

    /**
     * 创建 session.start 事件
     *
     * @param sessionId 会话 ID
     * @param requestId 请求 ID
     * @param traceId   追踪 ID
     * @return NDJSON 事件
     */
    public static NdjsonEvent createSessionStartEvent(String sessionId, String requestId, String traceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("messageId", UUID.randomUUID().toString());

        return NdjsonEvent.builder()
                .type("session.start")
                .payload(payload)
                .meta(createEventMeta(sessionId, requestId, traceId))
                .build();
    }

    /**
     * 创建 message.delta 事件（流式消息片段）
     *
     * @param messageId 消息 ID
     * @param delta     消息片段内容
     * @param sessionId 会话 ID
     * @param requestId 请求 ID
     * @param traceId   追踪 ID
     * @return NDJSON 事件
     */
    public static NdjsonEvent createMessageDeltaEvent(String messageId, String delta,
                                                      String sessionId, String requestId, String traceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", messageId);
        payload.put("delta", delta);

        return NdjsonEvent.builder()
                .type("message.delta")
                .payload(payload)
                .meta(createEventMeta(sessionId, requestId, traceId))
                .build();
    }

    /**
     * 创建 message.complete 事件（消息完成）
     *
     * @param messageId 消息 ID
     * @param sessionId 会话 ID
     * @param requestId 请求 ID
     * @param traceId   追踪 ID
     * @return NDJSON 事件
     */
    public static NdjsonEvent createMessageCompleteEvent(String messageId, String sessionId,
                                                         String requestId, String traceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", messageId);

        return NdjsonEvent.builder()
                .type("message.complete")
                .payload(payload)
                .meta(createEventMeta(sessionId, requestId, traceId))
                .build();
    }

    /**
     * 创建 ui.card 事件（UI 卡片）
     *
     * @param messageId 消息 ID
     * @param data      卡片数据
     * @param sessionId 会话 ID
     * @param requestId 请求 ID
     * @param traceId   追踪 ID
     * @return NDJSON 事件
     */
    public static NdjsonEvent createUiCardEvent(String messageId, Object data,
                                                String sessionId, String requestId, String traceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", messageId);

        // 如果 data 已经是 Map 类型，直接使用；否则转换为 Map
        if (data instanceof Map) {
            payload.put("data", data);
        } else if (data instanceof WeatherCard) {
            payload.put("data", convertWeatherCardToMap((WeatherCard) data));
        } else {
            // 其他情况尝试直接放入（依赖 Jackson 序列化）
            payload.put("data", data);
        }

        return NdjsonEvent.builder()
                .type("ui.card")
                .payload(payload)
                .meta(createEventMeta(sessionId, requestId, traceId))
                .build();
    }

    /**
     * 创建 ui.options 事件（UI 选项卡）
     *
     * @param messageId 消息 ID
     * @param quizCard  问答题卡片数据
     * @param sessionId 会话 ID
     * @param requestId 请求 ID
     * @param traceId   追踪 ID
     * @return NDJSON 事件
     */
    public static NdjsonEvent createUiOptionsEvent(String messageId, QuizCard quizCard,
                                                   String sessionId, String requestId, String traceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", messageId);

        // 构建 options 格式的数据
        Map<String, Object> optionsData = new HashMap<>();
        optionsData.put("question", quizCard.getQuestion());
        if (quizCard.getDescription() != null) {
            optionsData.put("description", quizCard.getDescription());
        }

        List<Map<String, Object>> options = new ArrayList<>();
        for (QuizCard.Option option : quizCard.getOptions()) {
            Map<String, Object> opt = new HashMap<>();
            opt.put("key", option.getKey());
            opt.put("value", option.getValue());
            opt.put("label", option.getKey() + ". " + option.getValue());
            if (option.getDescription() != null) {
                opt.put("description", option.getDescription());
            }
            options.add(opt);
        }
        optionsData.put("options", options);

        payload.put("data", optionsData);

        return NdjsonEvent.builder()
                .type("ui.options")
                .payload(payload)
                .meta(createEventMeta(sessionId, requestId, traceId))
                .build();
    }

    /**
     * 创建 ui.chart_bar 事件（UI 柱状图）
     *
     * @param messageId  消息 ID
     * @param chartCard  柱状图卡片数据
     * @param sessionId  会话 ID
     * @param requestId  请求 ID
     * @param traceId    追踪 ID
     * @return NDJSON 事件
     */
    public static NdjsonEvent createUiChartBarEvent(String messageId, BarChartCard chartCard,
                                                    String sessionId, String requestId, String traceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", messageId);
        payload.put("data", chartCard);

        return NdjsonEvent.builder()
                .type("ui.chart_bar")
                .payload(payload)
                .meta(createEventMeta(sessionId, requestId, traceId))
                .build();
    }

    /**
     * 根据工具类型创建对应的 UI 事件
     *
     * @param messageId 消息 ID
     * @param toolType  工具类型 (weather/quiz/chart)
     * @param data      工具数据
     * @param sessionId 会话 ID
     * @param requestId 请求 ID
     * @param traceId   追踪 ID
     * @return NDJSON 事件
     */
    public static NdjsonEvent createUiEvent(String messageId, String toolType, Object data,
                                            String sessionId, String requestId, String traceId) {
        log.info("[tool] 创建 UI 事件，类型：{}", toolType);

        return switch (toolType) {
            case "weather" -> createUiCardEvent(messageId, data, sessionId, requestId, traceId);
            case "quiz" -> {
                if (data instanceof String) {
                    data = JsonUtil.toObj(data.toString(), QuizCard.class);
                }
                yield createUiOptionsEvent(messageId, (QuizCard) data, sessionId, requestId, traceId);
            }
            case "chart" -> {
                if (data instanceof String) {
                    data = JsonUtil.toObj(data.toString(), BarChartCard.class);
                }
                yield createUiChartBarEvent(messageId, (BarChartCard) data, sessionId, requestId, traceId);
            }
            default -> createUiCardEvent(messageId, data, sessionId, requestId, traceId);
        };
    }

    /**
     * 创建 status.waiting_user 事件（等待用户输入）
     *
     * @param sessionId 会话 ID
     * @param requestId 请求 ID
     * @param traceId   追踪 ID
     * @return NDJSON 事件
     */
    public static NdjsonEvent createStatusWaitingUserEvent(String sessionId, String requestId, String traceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("enabled", true);

        return NdjsonEvent.builder()
                .type("status.waiting_user")
                .payload(payload)
                .meta(createEventMeta(sessionId, requestId, traceId))
                .build();
    }

    /**
     * 创建 session.end 事件（会话结束）
     *
     * @param sessionId 会话 ID
     * @param requestId 请求 ID
     * @param traceId   追踪 ID
     * @return NDJSON 事件
     */
    public static NdjsonEvent createSessionEndEvent(String sessionId, String requestId, String traceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);

        return NdjsonEvent.builder()
                .type("session.end")
                .payload(payload)
                .meta(createEventMeta(sessionId, requestId, traceId))
                .build();
    }

    /**
     * 创建 error 事件（错误响应）
     *
     * @param message   错误消息
     * @param traceId   追踪 ID
     * @param requestId 请求 ID
     * @param sessionId 会话 ID
     * @return NDJSON 事件
     */
    public static NdjsonEvent createErrorEvent(String message, String traceId, String requestId, String sessionId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("detail", message);

        return NdjsonEvent.builder()
                .type("error")
                .payload(payload)
                .meta(createEventMeta(sessionId, requestId, traceId))
                .build();
    }

    /**
     * 创建事件元数据
     *
     * @param sessionId 会话 ID
     * @param requestId 请求 ID
     * @param traceId   追踪 ID
     * @return 事件元数据
     */
    public static NdjsonEvent.EventMeta createEventMeta(String sessionId, String requestId, String traceId) {
        return NdjsonEvent.EventMeta.builder()
                .traceId(traceId)
                .requestId(requestId)
                .sessionId(sessionId)
                .sequence(sequenceGenerator.incrementAndGet())
                .eventId(UUID.randomUUID().toString())
                .ts(Instant.now().toString())
                .protocolVersion("2026-03-03")
                .build();
    }

    /**
     * 创建元数据 Map（用于非 NDJSON 事件的响应）
     *
     * @param sessionId 会话 ID
     * @param requestId 请求 ID
     * @param traceId   追踪 ID
     * @return 元数据 Map
     */
    public static Map<String, Object> createMetaMap(String sessionId, String requestId, String traceId) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("traceId", traceId);
        meta.put("requestId", requestId);
        meta.put("sessionId", sessionId);
        meta.put("protocolVersion", "2026-03-03");
        if (traceId != null) {
            meta.put("ts", Instant.now().toString());
        }
        return meta;
    }

    /**
     * 将 WeatherCard 转换为 Map
     *
     * @param weatherCard 天气卡片
     * @return Map 数据
     */
    private static Map<String, Object> convertWeatherCardToMap(WeatherCard weatherCard) {
        Map<String, Object> map = new HashMap<>();
        map.put("city", weatherCard.getCity());
        map.put("condition", weatherCard.getCondition());
        map.put("temperature", weatherCard.getTemperature());
        map.put("humidity", weatherCard.getHumidity());
        map.put("aqi", weatherCard.getAqi());
        map.put("windDirection", weatherCard.getWindDirection());
        map.put("windLevel", weatherCard.getWindLevel());
        map.put("dressAdvice", weatherCard.getDressAdvice());
        map.put("tips", weatherCard.getTips());
        return map;
    }
}
