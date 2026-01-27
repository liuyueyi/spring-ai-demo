package com.git.hui.springai.app;

import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootApplication
public class T01Application {

    public static void main(String[] args) {
        SpringApplication.run(T01Application.class, args);
    }

//    @Bean
//    @Primary
//    public ChatModel chatModel(Environment environment) {
//        log.info("init ChatModel ");
//        String key = environment.getProperty("spring.ai.openai.api-key");
//        String baseUrl = environment.getProperty("spring.ai.openai.base-url");
//        String model = environment.getProperty("spring.ai.openai.chat.options.model");
//        OpenAiApi.Builder builder = OpenAiApi.builder().apiKey(key)
//                .baseUrl("https://open.bigmodel.cn/api/paas")
//                .completionsPath("/v4/chat/completions");
//        OpenAiApi openAiApi = builder.build();
//        OpenAiChatOptions.Builder optionBuilder = OpenAiChatOptions.builder().model(model);
//        return OpenAiChatModel.builder().openAiApi(openAiApi).defaultOptions(optionBuilder.build()).build();
//    }

    @Bean
    CommandLineRunner commandLineRunner(ChatClient.Builder chatClientBuilder,
                                        @Value("${agent.skills.dirs:Unknown}") List<Resource> agentSkillsDirs) throws IOException {

        return args -> {

            ChatClient chatClient = chatClientBuilder // @formatter:off
                    .defaultSystem("始终运用现有技能协助用户满足其要求.")

                    // Skills tool
                    .defaultToolCallbacks(SkillsTool.builder().addSkillsResources(agentSkillsDirs).build())

                    .defaultTools(FileSystemTools.builder().build())
                    .defaultTools(ShellTools.builder().build())

                    .defaultAdvisors(
                            // Tool Calling advisor
                            ToolCallAdvisor.builder().build(),
                            // Custom logging advisor
                            MyLoggingAdvisor.builder()
                                    .showAvailableTools(false)
                                    .showSystemMessage(false)
                                    .build())
                    .defaultToolContext(Map.of("foo", "bar"))
                    .build();
            // @formatter:on

            var answer = chatClient
                    .prompt("""
                            按照最佳实际的方式，评审下面的代码实现:

                             D:\\Workspace\\hui\\project\\spring-ai-demo\\v2\\T01-agentic-skills-simple-design\\src\\main\\java\\com\\git\\hui\\springai\\app\\demo\\DocumentChunker.java
                             """)
                    .call()
                    .content();

            System.out.println("The Answer: " + answer);
        };

    }

}