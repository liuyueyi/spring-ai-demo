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
            ChatClient chatClient = chatClientBuilder
                    .defaultTools(AskUserQuestionTool.builder()
                            .questionHandler(new CommandLineQuestionHandler())
                            .build())

                    .defaultAdvisors(
                            // Tool calling advisor
                            ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
                            // Chat memory advisor - after the tool calling advisor to remember tool calls
                            MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(500).build()).build(),
                            // Custom logging advisor
                            MyLoggingAdvisor.builder()
                                    .showAvailableTools(false)
                                    .showSystemMessage(false)
                                    .build())
                    .build();

            // Start the chat loop
            System.out.println("\nI am your assistant.\n");


            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\n$->USER: ");
                    System.out.println("\n$->ASSISTANT: " + chatClient.prompt(scanner.nextLine())
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "113331"))
                            .call().content());
                }
            }
        };

    }

    public class CommandLineQuestionHandler implements AskUserQuestionTool.QuestionHandler {

        @Override
        public Map<String, String> handle(List<AskUserQuestionTool.Question> questions) {
            Map<String, String> answers = new HashMap<>();

            Scanner scanner = new Scanner(System.in);

            for (AskUserQuestionTool.Question q : questions) {
                System.out.println("\n" + q.header() + ": " + q.question());

                List<AskUserQuestionTool.Question.Option> options = q.options();
                for (int i = 0; i < options.size(); i++) {
                    AskUserQuestionTool.Question.Option opt = options.get(i);
                    System.out.printf("  %d. %s - %s%n", i + 1, opt.label(), opt.description());
                }

                if (q.multiSelect()) {
                    System.out.println("  (Enter numbers separated by commas, or type custom text)");
                } else {
                    System.out.println("  (Enter a number, or type custom text)");
                }

                String response = scanner.nextLine().trim();
                answers.put(q.question(), parseResponse(response, options));
            }

            return answers;
        }

        private static String parseResponse(String response, List<AskUserQuestionTool.Question.Option> options) {
            try {
                // Try parsing as option number(s)
                String[] parts = response.split(",");
                List<String> labels = new ArrayList<>();
                for (String part : parts) {
                    int index = Integer.parseInt(part.trim()) - 1;
                    if (index >= 0 && index < options.size()) {
                        labels.add(options.get(index).label());
                    }
                }
                return labels.isEmpty() ? response : String.join(", ", labels);
            } catch (NumberFormatException e) {
                // Not a number, use as free text
                return response;
            }
        }

    }

}