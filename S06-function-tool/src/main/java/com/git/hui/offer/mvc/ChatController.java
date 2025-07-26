package com.git.hui.offer.mvc;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * @author YiHui
 * @date 2025/7/26
 */
@RestController
public class ChatController {

    private final ChatClient chatClient;

    private final ChatModel chatModel;

    public ChatController(ZhiPuAiChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @RequestMapping(path = "showTime")
    public String showTime(String msg) {
        ToolCallback[] tools = ToolCallbacks.from(new DateTimeTools());
        ChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(tools)
                .build();
        return chatModel.call(new Prompt(msg, options)).getResult().getOutput().getText();
    }

    @RequestMapping(path = "time")
    public String getTime(String msg) {
        return chatClient.prompt(msg).tools(new DateTimeTools()).call().content();
    }

    @RequestMapping(path = "timeNoTools")
    public String getTimeNoTools(String msg) {
        return chatClient.prompt(msg).call().content();
    }

    @RequestMapping(path = "timeByCodeTool")
    public String getTimeByCodeTool(String msg) {
        Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getTimeByZoneId", ZoneId.class);

        ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("getTimeByZoneId")
                .description("传入时区，返回对应时区的当前时间给用户")
                .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
                .build();

        ToolMetadata toolMetadata = ToolMetadata.builder()
                .returnDirect(false)
                .build();

        ToolCallback callBack = MethodToolCallback.builder()
                .toolDefinition(toolDefinition)
                .toolMetadata(toolMetadata)
                .toolMethod(method)
                .toolObject(new DateTimeTools())
                .build();
        return chatClient.prompt(msg).toolCallbacks(callBack).call().content();
    }


    @RequestMapping(path = "timeByCodeFunc")
    public String getTimeByCodeFunc(String msg) {
        // 使用函数式工具需要注意的是，传参和返回结果，要么是void，要么是POJO
        ToolCallback callBack = FunctionToolCallback.builder("nowDateByArea", new NowService())
                .description("传入时区，返回对应时区的当前时间给用户")
                .inputType(AreaReq.class)
                // 下面这一行实际是可以省略的，默认就是根据 inputType 进行生成jsonSchema
                .inputSchema(JsonSchemaGenerator.generateForType(AreaReq.class))
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
        return chatClient.prompt(msg).toolCallbacks(callBack).call().content();
    }


    @RequestMapping(path = "timeByDeclareFunc")
    public String getTimeByDeclareFunc(String msg) {
        return chatClient.prompt(msg).tools("nowService").call().content();
    }

    class DateTimeTools {

        @Tool(description = "不需要关注用户时区，直接返回当前的时间给用户")
        String getCurrentDateTime() {
            String ans = LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
            System.out.println("进入获取当前时间了：" + ans);
            return ans;
        }

        @Tool(description = "传入时区，返回对应时区的当前时间给用户")
        String getTimeByZoneId(@ToolParam(description = "需要查询时间的时区") ZoneId area) {
            // 根据时区，查询对应的时间
            ZonedDateTime time = LocalDateTime.now().atZone(area);

            // 转换为 2025-07-26 20:00:00 格式的字符串
            // 将输入时区的时间转换为本地时区
            ZonedDateTime localTime = time.withZoneSameInstant(ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String ans = localTime.format(formatter);
            System.out.println("传入的时区是：" + area + "-" + ans);
            return ans;
        }
    }


    public static class NowService implements Function<AreaReq, AreaResp> {
        @Override
        public AreaResp apply(AreaReq req) {
            ZoneId area = req.zoneId();
            // 根据时区，查询对应的时间
            ZonedDateTime time = LocalDateTime.now().atZone(area);

            // 转换为 2025-07-26 20:00:00 格式的字符串
            // 将输入时区的时间转换为本地时区
            ZonedDateTime localTime = time.withZoneSameInstant(ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String ans = localTime.format(formatter);
            System.out.println("传入的时区是：" + area + "-" + ans);
            return new AreaResp(ans);
        }
    }

    public record AreaReq(@ToolParam(description = "需要查询时间的时区") ZoneId zoneId) {
    }

    public record AreaResp(String time) {
    }
}
