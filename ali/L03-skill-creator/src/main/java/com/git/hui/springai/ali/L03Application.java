package com.git.hui.springai.ali;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.alibaba.cloud.ai.graph.agent.tools.WebFetchTool;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author YiHui
 * @date 2026/3/6
 */
@Controller
@SpringBootApplication
public class L03Application {
    public static void main(String[] args) {
        SpringApplication.run(L03Application.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(ChatModel chatModel) throws IOException {
        return args -> {
            // 使用 ClasspathSkillRegistry 注册技能, 会扫描资源目录中 skills 目录下的技能
            // 除了使用 ClasspathSkillRegistry 注册技能，也可以使用 FileSystemSkillRegistry 从指定目录中加载技能
            SkillRegistry registry = ClasspathSkillRegistry.builder()
                    .classpathPath("skills")
                    .build();



            // 需要注意的是，若skills的节能若需要配合脚本执行（如 python 脚本） shell命令，则通常还需要我们主动注册对应的工具能力
            // Shell Hook：提供 Shell 命令执行（工作目录可指定，如当前工程目录）
            ShellToolAgentHook shellHook = ShellToolAgentHook.builder()
                    .shellTool2(ShellTool2.builder(System.getProperty("user.dir")).build())
                    .build();
            // 如果需要执行python 脚本，则需要注册 python 工具
            // 如谁用alibaba示例项目中的
            // ToolCallback toolCallback = PythonTool.createPythonToolCallback(PythonTool.DESCRIPTION)


            // 通过将工具与 Skill 技能名绑定，可以做到工具跟随 Skill 实现渐进式披露：仅当模型对该技能调用了 read_skill 后，对应工具才会加入当次请求，实现按需暴露。激活后该技能的工具在会话后续轮次中仍可用。
//            Map<String, List<ToolCallback>> groupedTools = Map.of(
//                    "technical-writing",   // 与 SKILL.md 的 name 一致，如 'blog_creator'
//                    List.of(webFetchTool)
//            );

            // SkillsAgentHook 注册 read_skill 工具并注入技能列表到系统提示，模型在需要时调用 read_skill(skill_name) 按需加载完整内容
            SkillsAgentHook hook = SkillsAgentHook.builder()
                    .skillRegistry(registry)
                    // 渐进式工具 Tool 纰漏
//                    .groupedTools(groupedTools)
                    .build();

            var webFetchTool = WebFetchTool.builder(ChatClient.builder(chatModel).build())
                    .withName("web-fetcher").withDescription("这是一个网络查询的工具，当你需要从网络上进行搜索相关信息时，使用这个工具").build();


            ReactAgent agent = ReactAgent.builder()
                    .name("skills-agent")
                    .model(chatModel)
                    .systemPrompt("""
                              你是一个专业的协作助手，当需要进行创作、写文章、写诗歌，你可以通过 read_skill 工具来获取技能
                            """)
                    .enableLogging(true)
                    .saver(new MemorySaver())
                    .tools(webFetchTool)
                    .hooks(List.of(hook, shellHook))
                    .build();

            // 会调用 web-fetcher 工具查询网页信息，然后
            AssistantMessage msg = agent.call("""
                    帮我写一篇关于 ReAct原理 的介绍文章
                    在开始之前，请先使用web-fetcher从 https://www.ppai.top/ai-guides/tutorial/hello-agent/03.Agent%E6%80%9D%E8%80%83%E6%A1%86%E6%9E%B6-ReAct.html 中搜索相关的素材作为博文的准备材料；准备完成之后，直接帮我生成对应的博文内容
                    """);
//            AssistantMessage msg = agent.call("帮我写一首关于爱情的诗");
            System.out.println(msg.getText());
        };
    }
}