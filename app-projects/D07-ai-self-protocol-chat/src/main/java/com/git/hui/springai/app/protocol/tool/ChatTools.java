package com.git.hui.springai.app.protocol.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 对话协议工具集
 * <p>
 * 提供三个工具：
 * 1. 天气查询 - 返回 ui.card 卡片
 * 2. 知识问答 - 返回 ui.options 选项卡
 * 3. 数据对比 - 返回 ui.chart_bar 柱状图
 *
 * @author YiHui
 * @date 2026/3/5
 */
@Component
public class ChatTools {
    private static final Logger log = LoggerFactory.getLogger(ChatTools.class);

    private final ObjectMapper objectMapper;

    public ChatTools(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 查询天气
     *
     * @param city 城市名称
     * @param toolContext 工具上下文（可选）
     * @return 天气卡片数据
     */
    @Tool(description = "查询指定城市的天气信息，返回详细的天气状况、温度、湿度等数据")
    @ToolResponseType("card")  // 声明返回类型为 card
    public WeatherCard queryWeather(
            @ToolParam(description = "城市名称，如北京、上海、广州等") String city,
            ToolContext toolContext) {  // ✅ 添加工具上下文参数

        // 从上下文中获取额外信息
        if (toolContext != null && !toolContext.getContext().isEmpty()) {
            log.info("【工具上下文】queryWeather - context: {}", toolContext.getContext());
            // 可以从中获取 userId, sessionId, appId 等信息
            String userId = (String) toolContext.getContext().get("userId");
            String sessionId = (String) toolContext.getContext().get("sessionId");
            log.info("用户 {} 在会话 {} 中查询 {} 的天气", userId, sessionId, city);
        }

        log.info("[inner-tool] 查询天气：{}", city);

        // TODO: 实际场景中应该调用天气 API
        // 这里使用模拟数据演示
        Random random = new Random();
        int temperature = 15 + random.nextInt(20); // 15-35 度
        int humidity = 40 + random.nextInt(40);    // 40-80%
        int aqi = 20 + random.nextInt(80);         // 20-100

        String[] conditions = {"晴", "多云", "阴", "小雨", "大雨"};
        String condition = conditions[random.nextInt(conditions.length)];

        String[] directions = {"东风", "南风", "西风", "北风", "东南风", "东北风"};
        String windDirection = directions[random.nextInt(directions.length)];
        String windLevel = (random.nextInt(5) + 1) + "级";

        String dressAdvice = getDressAdvice(temperature, condition);
        String tips = getWeatherTips(condition, aqi);

        return WeatherCard.builder()
                .city(city)
                .condition(condition)
                .temperature(temperature)
                .humidity(humidity)
                .aqi(aqi)
                .windDirection(windDirection)
                .windLevel(windLevel)
                .dressAdvice(dressAdvice)
                .tips(tips)
                .build();
    }

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

    /**
     * 数据对比（柱状图）
     *
     * @param title      图表标题
     * @param categories 对比类别
     * @param data       对比数据
     * @return 柱状图卡片
     */
    @Tool(description = "对比多个数据项并以柱状图形式展示，适用于销售对比、业绩对比等场景")
    @ToolResponseType("chart")  // 声明返回类型为 chart
    public BarChartCard compareData(
            @ToolParam(description = "图表标题") String title,
            @ToolParam(description = "X 轴分类标签列表") List<String> categories,
            @ToolParam(description = "数据集，键为系列名称，值为数据列表") Map<String, List<Integer>> data) {
        log.info("[inner-tool] 数据对比：{}", title);

        List<BarChartCard.Dataset> datasets = new ArrayList<>();
        int datasetIndex = 0;

        for (Map.Entry<String, List<Integer>> entry : data.entrySet()) {
            String color = getColorByIndex(datasetIndex);
            datasets.add(BarChartCard.Dataset.builder()
                    .label(entry.getKey())
                    .data(entry.getValue())
                    .backgroundColor(color)
                    .borderColor(darkenColor(color))
                    .build());
            datasetIndex++;
        }

        return BarChartCard.builder()
                .title(title)
                .xAxis(categories)
                .datasets(datasets)
                .yAxisLabel("数值")
                .showLabels(true)
                .colorScheme(BarChartCard.ColorScheme.DEFAULT)
                .build();
    }

    /**
     * 获取所有工具回调
     */
    public List<ToolCallback> getTools() {
        ToolCallback[] toolCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(this)
                .build()
                .getToolCallbacks();
        return List.of(toolCallbacks);
    }

    // ========== 内部辅助方法 ==========

    private String getDressAdvice(int temperature, String condition) {
        if (temperature < 10) {
            return "建议穿厚外套或羽绒服，注意保暖";
        } else if (temperature < 20) {
            return "建议穿长袖衬衫或薄外套";
        } else if (temperature < 28) {
            return "建议穿短袖 T 恤，清凉透气";
        } else {
            return "建议穿清凉夏装，注意防暑降温";
        }
    }

    private String getWeatherTips(String condition, int aqi) {
        StringBuilder tips = new StringBuilder();

        switch (condition) {
            case "晴":
                tips.append("阳光明媚，适合户外活动。");
                break;
            case "多云":
                tips.append("云层较多，紫外线适中。");
                break;
            case "阴":
                tips.append("天气阴沉，请保持好心情。");
                break;
            case "小雨":
                tips.append("有小雨，出门请带伞。");
                break;
            case "大雨":
                tips.append("雨势较大，尽量减少外出。");
                break;
        }

        if (aqi <= 50) {
            tips.append("空气质量优，适合户外运动。");
        } else if (aqi <= 100) {
            tips.append("空气质量良好，可正常活动。");
        } else {
            tips.append("空气质量较差，敏感人群减少外出。");
        }

        return tips.toString();
    }

    private String getColorByIndex(int index) {
        String[] colors = {
                "#667eea",   // 紫色
                "#f093fb",   // 粉色
                "#4facfe",   // 蓝色
                "#43e97b",   // 绿色
                "#fa709a",   // 玫红
                "#fee140",   // 黄色
                "#30cfd0",   // 青色
                "#a8edea"    // 淡青
        };
        return colors[index % colors.length];
    }

    private String darkenColor(String hexColor) {
        // 简单实现：将颜色变暗 20%
        try {
            int r = Integer.parseInt(hexColor.substring(1, 3), 16);
            int g = Integer.parseInt(hexColor.substring(3, 5), 16);
            int b = Integer.parseInt(hexColor.substring(5, 7), 16);

            r = Math.max(0, r - 51);
            g = Math.max(0, g - 51);
            b = Math.max(0, b - 51);

            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return hexColor;
        }
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

    // ========== 数据结构 ==========

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
