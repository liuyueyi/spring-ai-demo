package com.git.hui.springai.app;

import com.git.hui.springai.app.advisor.MyLoggingAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Slf4j
@SpringBootApplication
public class T02Application {

    public static void main(String[] args) {
        SpringApplication.run(T02Application.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(ChatClient.Builder chatClientBuilder) throws IOException {


        return args -> {
            // 构建ChatClient实例，配置所需的工具和顾问
            ChatClient chatClient = chatClientBuilder
                    // 设置默认工具，这里使用AskUserQuestionTool，它允许AI向用户提问
                    .defaultTools(AskUserQuestionTool.builder()
                            // 使用命令行问题处理器来处理AI向用户提出的问题
                            .questionHandler(new CommandLineQuestionHandler())
                            .build())

                    // 配置默认的顾问（拦截器），按顺序执行
                    .defaultAdvisors(
                            // 工具调用顾问：处理工具调用逻辑
                            ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
                            
                            // 消息记忆顾问：维护对话历史，使用消息窗口聊天记忆
                            // 位于工具调用顾问之后，以确保能够记住工具调用结果
                            MessageChatMemoryAdvisor.builder(
                                MessageWindowChatMemory.builder()
                                    .maxMessages(500)  // 最多保存500条消息
                                    .build())
                            .build(),
                            
                            // 自定义日志顾问：记录对话过程，可控制是否显示可用工具和系统消息
                            MyLoggingAdvisor.builder()
                                    .showAvailableTools(true)   // 不显示可用工具列表
                                    .showSystemMessage(true)    // 不显示系统消息
                                    .build())
                    .build();

            // 启动聊天循环的提示信息
            System.out.println("\nI am your assistant.\n");

            // 使用try-with-resources确保Scanner资源得到正确释放
            try (Scanner scanner = new Scanner(System.in)) {
                // 无限循环，持续接收用户输入并返回AI回复
                while (true) {
                    System.out.print("\n$->USER: ");  // 提示用户输入
                    
                    // 获取用户输入，通过chatClient发送请求，指定对话ID，并输出AI回复
                    System.out.println("\n$->ASSISTANT: " + 
                            chatClient.prompt(scanner.nextLine())  // 发送用户输入到AI
                                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "113331"))  // 设置对话ID以维护上下文
                                    .call().content());  // 执行调用并获取AI回复内容
                }
            }
        };

    }

    /**
     * 命令行问题处理器，实现 AskUserQuestionTool.QuestionHandler 接口
     * 用于处理AI工具向用户提出的问题，支持单选、多选和自定义文本输入
     */
    public class CommandLineQuestionHandler implements AskUserQuestionTool.QuestionHandler {

        /**
         * 处理传入的问题列表，逐个显示问题和选项，收集用户输入的答案
         *
         * @param questions 需要处理的问题列表
         * @return 包含问题和对应答案的映射表
         */
        @Override
        public Map<String, String> handle(List<AskUserQuestionTool.Question> questions) {
            Map<String, String> answers = new HashMap<>();

            Scanner scanner = new Scanner(System.in);

            for (AskUserQuestionTool.Question q : questions) {
                // 显示问题标题和内容
                System.out.println("\n" + q.header() + ": " + q.question());

                // 获取并显示选项
                List<AskUserQuestionTool.Question.Option> options = q.options();
                for (int i = 0; i < options.size(); i++) {
                    AskUserQuestionTool.Question.Option opt = options.get(i);
                    System.out.printf("  %d. %s - %s%n", i + 1, opt.label(), opt.description());
                }

                // 根据是否为多选显示提示信息
                if (q.multiSelect()) {
                    System.out.println("  (Enter numbers separated by commas, or type custom text)");
                } else {
                    System.out.println("  (Enter a number, or type custom text)");
                }

                // 读取用户输入并解析答案
                String response = scanner.nextLine().trim();
                answers.put(q.question(), parseResponse(response, options));
            }

            return answers;
        }

        /**
         * 解析用户输入的响应，根据选项索引或自定义文本返回对应的值
         *
         * @param response 用户输入的原始字符串
         * @param options  可用的选项列表
         * @return 解析后的答案字符串
         */
        private static String parseResponse(String response, List<AskUserQuestionTool.Question.Option> options) {
            try {
                // 尝试将输入解析为选项编号（支持多个选项）
                String[] parts = response.split(",");
                List<String> labels = new ArrayList<>();
                for (String part : parts) {
                    int index = Integer.parseInt(part.trim()) - 1;
                    if (index >= 0 && index < options.size()) {
                        labels.add(options.get(index).label());
                    }
                }
                // 如果没有找到有效的选项编号，则返回原始输入；否则返回选项标签的组合
                return labels.isEmpty() ? response : String.join(", ", labels);
            } catch (NumberFormatException e) {
                // 如果输入不是数字，则将其视为自由文本
                return response;
            }
        }
    }

}