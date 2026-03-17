package com.git.hui.springai.ali.planer;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 *
 * @author YiHui
 * @date 2026/3/17
 */
@Service
public class ParallelPlanAgent {

    public static final String PLAN_AGENT = "parallel_plan_agent";

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private BudgetAgent budgetAgent;
    @Autowired
    private CreativeAgent creativeAgent;
    @Autowired
    private ExecutionAgent executionAgent;

    public ParallelAgent planAgent() {
        ParallelAgent agent = ParallelAgent.builder()
                .name(PLAN_AGENT)
                .description("多角度方案策划，可以从创意、财务、执行等各方面进行方案策划，通过并行调用 creative_agent、budget_agent、execution_agent来生成制定计划所需要的内容，最终输出完整的计划方案给用户")
                .subAgents(List.of(budgetAgent.budgetAgent(), creativeAgent.creativeAgent(), executionAgent.executionAgent()))
                // 如果只调用并行策略，我们可以通过自定义的合并策略，拿之前Agent的输出，来构建完整的输出
//                .mergeStrategy(new PlanMergeStrategy(chatModel))
                // 默认的合并策略，对于先并行，后串行的使用场景，可以使用默认的策略
                .mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
                .maxConcurrency(3)
                .mergeOutputKey("complete_plan")
                .build();
        return agent;
    }

    public class PlanMergeStrategy implements ParallelAgent.MergeStrategy {

        private final ChatModel chatModel;

        public PlanMergeStrategy(ChatModel chatModel) {
            this.chatModel = chatModel;
        }

        @Override
        public Object merge(Map<String, Object> mergedState, OverAllState overallState) {
            // 获取各个子 Agent 的输出
            String creativeIdeas = ((AssistantMessage) mergedState.get("creative_ideas")).getText();
            String budgetPlan = ((AssistantMessage) mergedState.get("budget_plan")).getText();
            String executionSteps = ((AssistantMessage) mergedState.get("execution_steps")).getText();

            // 获取用户原始输入
            String userInput = (String) overallState.value("input").orElse("");

            // 构建合并提示词
            String mergePrompt = buildMergePrompt(userInput, creativeIdeas, budgetPlan, executionSteps);

            // 调用大模型生成完整方案
            try {
                String completePlan = chatModel.call(mergePrompt);

                // 返回包含所有信息的 Map
                mergedState.put("complete_plan", completePlan);
                return mergedState;
            } catch (Exception e) {
                // 如果合并失败，返回原始结果
                return mergedState;
            }
        }

        /**
         * 构建合并提示词
         */
        private String buildMergePrompt(String userInput, String creativeIdeas, String budgetPlan, String executionSteps) {
            return String.format("""
                            你是一位专业的方案策划顾问，请将以下三个方面的内容整合成一份完整、专业的策划方案。
                                                
                            【活动主题】
                            %s
                                                
                            【创意点子】
                            %s
                                                
                            【预算规划】
                            %s
                                                
                            【执行步骤】
                            %s
                                                
                            ---
                            请将以上内容整合为一份结构清晰、内容完整的策划方案，要求：
                            1. 包含活动背景和目标
                            2. 详细列出创意亮点
                            3. 清晰的预算分配
                            4. 具体的执行计划和时间节点
                            5. 使用专业的格式和排版
                            6. 语言简洁明了，易于理解
                                                
                            直接返回完整的策划方案文档。
                            """,
                    userInput != null ? userInput : "未知主题",
                    creativeIdeas != null ? creativeIdeas : "暂无创意点子",
                    budgetPlan != null ? budgetPlan : "暂无预算规划",
                    executionSteps != null ? executionSteps : "暂无执行步骤"
            );
        }
    }


    public SequentialAgent seqPlanAgent() {
        SequentialAgent agent = SequentialAgent.builder()
                .name("方案策划专家")
                .description("根据用户给的信息，完成整体的方案策划")
                .subAgents(List.of(planAgent(), completeAgent()))
                .build();
        return agent;
    }

    public ReactAgent completeAgent() {
        ReactAgent agent = ReactAgent.builder()
                .name("complete_agent")
                .model(chatModel)
                .description("完成方案制定Agent")
                .instruction("""
                        你是一位专业的方案策划顾问，请将以下三个方面的内容整合成一份完整、专业的策划方案。
                                                
                            【活动主题】
                            {input}
                                                
                            【创意点子】
                            {creative_ideas}
                                                
                            【预算规划】
                            {budget_plan}
                                                
                            【执行步骤】
                            {execution_steps}
                                                
                            ---
                            请将以上内容整合为一份结构清晰、内容完整的策划方案，要求：
                            1. 包含活动背景和目标
                            2. 详细列出创意亮点
                            3. 清晰的预算分配
                            4. 具体的执行计划和时间节点
                            5. 使用专业的格式和排版
                            6. 语言简洁明了，易于理解
                                                
                            直接返回完整的策划方案文档。
                        """)
                .outputKey("complete_plan")
                .includeContents(false)
                .returnReasoningContents(false)
                .enableLogging(true)
                .build();
        return agent;
    }
}
