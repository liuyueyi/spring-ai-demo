package com.git.hui.springai.tools;

import com.git.hui.springai.tools.dto.QuizCard;
import com.git.hui.springai.tools.dto.WeatherCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author YiHui
 * @date 2026/3/6
 */
@Slf4j
@Component
public class QuizTools {

//    同名的工具，在调用时会报错 -- java.lang.IllegalStateException: Multiple tools with the same name (queryWeather) found in ToolCallingChatOptions
//    @Tool(name = "queryWeather", description = "查询指定国家的天气信息，返回详细的天气状况、温度、湿度等数据")
//    @ToolResponseType("card")  // 声明返回类型为 card
//    public WeatherCard fetchWeather(
//            @ToolParam(description = "城市名称，如北京、上海、广州等") String city,
//            ToolContext toolContext) {  // ✅ 添加工具上下文参数
//
//        // 从上下文中获取额外信息
//        if (toolContext != null && !toolContext.getContext().isEmpty()) {
//            log.info("【工具上下文】queryWeather - context: {}", toolContext.getContext());
//            // 可以从中获取 userId, sessionId, appId 等信息
//            String userId = (String) toolContext.getContext().get("userId");
//            String sessionId = (String) toolContext.getContext().get("sessionId");
//            log.info("用户 {} 在会话 {} 中查询 {} 的天气", userId, sessionId, city);
//        }
//
//        log.info("[inner-tool] 查询天气：{}", city);
//
//        // TODO: 实际场景中应该调用天气 API
//        // 这里使用模拟数据演示
//        Random random = new Random();
//        int temperature = 15 + random.nextInt(20); // 15-35 度
//        int humidity = 40 + random.nextInt(40);    // 40-80%
//        int aqi = 20 + random.nextInt(80);         // 20-100
//
//        String[] conditions = {"晴", "多云", "阴", "小雨", "大雨"};
//        String condition = conditions[random.nextInt(conditions.length)];
//
//        String[] directions = {"东风", "南风", "西风", "北风", "东南风", "东北风"};
//        String windDirection = directions[random.nextInt(directions.length)];
//        String windLevel = (random.nextInt(5) + 1) + "级";
//
//
//        return WeatherCard.builder()
//                .city(city)
//                .condition(condition)
//                .temperature(temperature)
//                .humidity(humidity)
//                .aqi(aqi)
//                .windDirection(windDirection)
//                .windLevel(windLevel)
//                .dressAdvice("")
//                .tips("")
//                .build();
//    }

    /**
     * 知识问答
     *
     * @param topic 主题
     * @return 问答题卡片
     */
    @Tool(description = "创建知识问答题目，支持多个主题领域，返回问题和候选项")
    @ToolResponseType("quiz")  // 声明返回类型为 quiz
    public QuizCard createQuiz(@ToolParam(description = "问题主题，如 spring、ai、地理等") String topic) {
        log.info("[inner-tool] 创建知识问答：{}", topic);

        // TODO: 实际场景中应该根据主题动态生成题目
        // 这里使用预设的示例题目
        Map<String, QuizData> quizBank = initQuizBank();
        QuizData quizData = quizBank.getOrDefault(topic.toLowerCase(), quizBank.get("default"));

        return QuizCard.builder()
                .question(quizData.question)
                .description(quizData.description)
                .options(quizData.options)
                .correctAnswer(quizData.correctAnswer)
                .explanation(quizData.explanation)
                .difficulty(quizData.difficulty)
                .build();
    }

    private Map<String, QuizData> initQuizBank() {
        Map<String, QuizData> quizBank = new HashMap<>();

        // 默认题目
        quizBank.put("default", QuizData.builder()
                .question("中国的首都是哪里？")
                .description("这是一道基础地理题")
                .options(Arrays.asList(
                        QuizCard.Option.builder().key("A").value("上海").build(),
                        QuizCard.Option.builder().key("B").value("北京").build(),
                        QuizCard.Option.builder().key("C").value("广州").build(),
                        QuizCard.Option.builder().key("D").value("深圳").build()
                ))
                .correctAnswer("B")
                .explanation("北京是中国的首都，位于华北平原北部。")
                .difficulty(QuizCard.Difficulty.EASY)
                .build());

        // Spring 相关题目
        quizBank.put("spring", QuizData.builder()
                .question("Spring AI 中，用于构建流式响应的核心接口是？")
                .description("考察 Spring AI 基础知识")
                .options(Arrays.asList(
                        QuizCard.Option.builder().key("A").value("ChatClient").build(),
                        QuizCard.Option.builder().key("B").value("Flux").build(),
                        QuizCard.Option.builder().key("C").value("StreamBuilder").build(),
                        QuizCard.Option.builder().key("D").value("ResponseEmitter").build()
                ))
                .correctAnswer("A")
                .explanation("ChatClient 是 Spring AI 的核心接口，通过.stream() 方法可以构建流式响应。")
                .difficulty(QuizCard.Difficulty.MEDIUM)
                .build());

        // AI 相关题目
        quizBank.put("ai", QuizData.builder()
                .question("大语言模型（LLM）中的'LLM'代表什么？")
                .description("考察 AI 基础概念")
                .options(Arrays.asList(
                        QuizCard.Option.builder().key("A").value("Large Learning Machine").build(),
                        QuizCard.Option.builder().key("B").value("Language Learning Model").build(),
                        QuizCard.Option.builder().key("C").value("Large Language Model").build(),
                        QuizCard.Option.builder().key("D").value("Logic Language Model").build()
                ))
                .correctAnswer("C")
                .explanation("LLM = Large Language Model，即大语言模型，是基于海量文本数据训练的深度学习模型。")
                .difficulty(QuizCard.Difficulty.EASY)
                .build());

        return quizBank;
    }

    @lombok.Data
    @lombok.Builder
    static class QuizData {
        String question;
        String description;
        List<QuizCard.Option> options;
        String correctAnswer;
        String explanation;
        QuizCard.Difficulty difficulty;
    }
}
