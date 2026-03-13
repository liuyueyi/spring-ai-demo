package com.git.hui.springai.ali;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.git.hui.springai.ali.express.HandoffExpressOrderHook;
import com.git.hui.springai.ali.express.tools.ExpressOrderTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author YiHui
 * @date 2026/3/6
 */
@Slf4j
@Controller
@SpringBootApplication
public class L04Application {
    public static void main(String[] args) {
        SpringApplication.run(L04Application.class, args);
    }

    private static final String THREAD_ID = "handoffs-demo-thread";

    @Bean
    public MemorySaver memorySaver() {
        return new MemorySaver();
    }

//    @Bean
    CommandLineRunner commandLineRunner(ChatModel chatModel, MemorySaver memorySaver) throws IOException {
        // 获取快递下单工具
        List<ToolCallback> expressTools = ExpressOrderTools.getTOOLS();

        // 创建快递下单 Hook，提供分步流程控制
        HandoffExpressOrderHook expressHook = new HandoffExpressOrderHook();

        ReactAgent expressAgent = ReactAgent.builder()
                .name("express_order_agent")
                .model(chatModel)
                .tools(expressTools)
                .saver(memorySaver)
                .hooks(expressHook)  // 使用 Hook 进行分步控制
                .enableLogging(true)
                .build();

        return args -> {
            RunnableConfig config = RunnableConfig.builder().threadId(THREAD_ID).build();

            // Turn 1: 用户发起下单请求
            log.info("=== Turn 1: 用户发起快递下单请求 ===");
            AssistantMessage r1 = expressAgent.call(new UserMessage("我要寄快递"), config);
            log.info("Assistant: {}", r1.getText());

            // Turn 2: 用户提供收件人信息
            log.info("\n=== Turn 2: 用户提供收件人信息 ===");
            AssistantMessage r2 = expressAgent.call(new UserMessage("收件人：张三，电话：13800138000，地址：北京市朝阳区某某街道 1 号"), config);
            log.info("Assistant: {}", r2.getText());

            // Turn 3: 用户提供发件人信息
            log.info("\n=== Turn 3: 用户提供发件人信息 ===");
            AssistantMessage r3 = expressAgent.call(new UserMessage("发件人：李四，电话：13900139000，地址：上海市浦东新区某某路 2 号"), config);
            log.info("Assistant: {}", r3.getText());

            // Turn 4: 用户提供物品信息
            log.info("\n=== Turn 4: 用户提供物品信息 ===");
            AssistantMessage r4 = expressAgent.call(new UserMessage("我要寄五本书，不用保险，普通快递即可"), config);
            log.info("Assistant: {}", r4.getText());

            // Turn 5: 查看订单信息
            log.info("\n=== Turn 5: 查看订单信息 ===");
            AssistantMessage r5 = expressAgent.call(new UserMessage("查看订单信息"), config);
            log.info("Assistant: {}", r5.getText());

            // Turn 6: 创建订单
            log.info("\n=== Turn 6: 创建订单 ===");
            AssistantMessage r6 = expressAgent.call(new UserMessage("确认下单"), config);
            log.info("Assistant: {}", r6.getText());
        };
    }
}