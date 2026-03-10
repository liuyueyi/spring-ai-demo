package com.git.hui.springai.ali.mvc;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.git.hui.springai.ali.mvc.tool.CurdTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author YiHui
 * @date 2026/3/9
 */
@RestController
public class ChatController {
    private final ChatModel chatModel;

    private final MemorySaver memorySaver;

    private ReactAgent reactAgent;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CurdTool curdTool;

    // 存储等待审批的会话状态
    private final Map<String, PendingApproval> pendingApprovals = new ConcurrentHashMap<>();

    // 存储工具执行结果的缓存
    private final Map<String, Object> toolResultCache = new ConcurrentHashMap<>();

    public ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
        // 配置检查点保存器（人工介入需要检查点来处理中断）
        this.memorySaver = MemorySaver.builder().build();
        this.reactAgent = initAgent();
    }

    /**
     * 待审批的会话信息
     */
    private static class PendingApproval {
        InterruptionMetadata interruptionMetadata;
        RunnableConfig config;
        String msg;
        String toolResultKey; // 工具执行结果的缓存 key

        public PendingApproval(InterruptionMetadata interruptionMetadata, RunnableConfig config, String msg, String toolResultKey) {
            this.interruptionMetadata = interruptionMetadata;
            this.config = config;
            this.msg = msg;
            this.toolResultKey = toolResultKey;
        }
    }

    /**
     * 初始化 Agent，配置 CURD 工具和人工审批机制
     */
    private ReactAgent initAgent() {
        // 创建 CURD 工具 - 支持完整的增删改查
        ToolCallback curdToolCallback = FunctionToolCallback.builder("execute_sql", (Map<String, Object> args) -> {
                    String sql = (String) args.get("sql");
                    System.out.println("[CURD 工具] 执行 SQL: " + sql);

                    // 1. 安全性检查
                    if (!curdTool.isSqlSafe(sql)) {
                        return "❌ 错误：SQL 语句包含危险操作（DROP/TRUNCATE/ALTER 等），已被安全拦截！";
                    }

                    // 2. 执行 SQL 操作
                    @SuppressWarnings("unchecked")
                    List<Object> params = (List<Object>) args.getOrDefault("params", null);

                    Map<String, Object> result = curdTool.execute(jdbcTemplate, sql, params);

                    // 3. 格式化返回结果
                    String formattedResult = curdTool.formatResult(result);

                    // 4. 缓存执行结果（用于审批后返回）
                    // 注意：这里只是临时缓存，真正的缓存需要在检测到中断时进行
                    System.out.println("[CURD 工具] 执行结果：" + formattedResult);

                    return formattedResult;
                })
                .description("执行 SQL 操作（支持 SELECT/INSERT/UPDATE/DELETE）。参数：sql - SQL 语句，params - 参数列表（可选）")
                .inputType(Map.class)
                .build();

        // 创建人工介入 Hook，对 SQL 执行工具添加审批机制
        HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
                .approvalOn("execute_sql", ToolConfig.builder()
                        .description("⚠️ SQL 执行操作需要审批！请确认 SQL 语句的安全性和正确性：")
                        .build())
                .build();

        // 创建 Agent
        ReactAgent agent = ReactAgent.builder()
                .name("sql_query_agent")
                .model(chatModel)
                .instruction("""
                        你现在是一个智能 SQL 助手，可以访问订单数据库（表名：t_order）。
                            
                        【表结构说明】
                        - id: 订单 ID（主键，自增）
                        - order_no: 订单号（唯一）
                        - customer_name: 客户姓名
                        - customer_email: 客户邮箱
                        - product_name: 产品名称
                        - quantity: 购买数量
                        - unit_price: 商品单价
                        - total_amount: 订单总金额
                        - status: 订单状态（已完成/待发货/待支付）
                        - create_time: 创建时间
                        - update_time: 更新时间
                            
                        【可用操作】
                        你可以使用 execute_sql 工具执行以下 SQL 操作：
                        1. SELECT - 查询订单数据
                        2. INSERT - 插入新订单
                        3. UPDATE - 更新订单信息
                        4. DELETE - 删除订单
                            
                        【使用示例】
                        - 查询：SELECT * FROM t_order WHERE customer_name = ?
                        - 插入：INSERT INTO t_order (order_no, customer_name, product_name, quantity, unit_price, total_amount, status) VALUES (?, ?, ?, ?, ?, ?, ?)
                        - 更新：UPDATE t_order SET status = ? WHERE order_no = ?
                        - 删除：DELETE FROM t_order WHERE order_no = ?
                                                
                        【参数说明】
                        如果 SQL 中包含 ? 占位符，需要在 params 参数中按顺序提供对应的值
                        例如：SELECT * FROM t_order WHERE customer_name = ?  对应的 params: ["张三"]
                            
                        【重要提示】
                        1. 所有 SQL 操作都需要人工审批确认
                        2. 建议使用参数化查询防止 SQL 注入
                        3. 查询时使用 LIMIT 限制返回数量
                        4. 执行更新/删除前务必确认 WHERE 条件
                        5. 禁止使用 DROP、TRUNCATE、ALTER 等危险操作
                        """)
                .tools(curdToolCallback)
                .hooks(List.of(humanInTheLoopHook))
                .saver(memorySaver)
                .build();

        return agent;
    }

    @GetMapping("/sql")
    public Map<String, Object> chat(String msg, String sessionId) throws GraphRunnerException {
        Map<String, Object> response = new HashMap<>();

        // 使用 sessionId 作为线程 ID，保证对话上下文
        String threadId = sessionId != null ? sessionId : "default-session";

        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 第一次调用 - 可能触发中断
        System.out.println("=== 第一次调用：用户请求 ===");
        Optional<NodeOutput> result = reactAgent.invokeAndGetOutput(msg, config);

        // 检查是否返回了中断（需要人工审批）
        if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
            InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();
            System.out.println("[中断] 描述：" + interruptionMetadata);

            // 获取需要审批的工具调用信息
            List<InterruptionMetadata.ToolFeedback> toolFeedbacks = interruptionMetadata.toolFeedbacks();

            System.out.println("\n=== 检测到中断，需要人工审批 ===");
            for (InterruptionMetadata.ToolFeedback feedback : toolFeedbacks) {
                System.out.println("工具名称：" + feedback.getName());
                System.out.println("工具参数：" + feedback.getArguments());
                System.out.println("审批描述：" + feedback.getDescription());
            }

            // 保存待审批状态
            String toolResultKey = "result_" + threadId + "_" + System.currentTimeMillis();
            pendingApprovals.put(threadId, new PendingApproval(interruptionMetadata, config, msg, toolResultKey));

            // 返回审批信息给前端
            response.put("status", "pending_approval");
            response.put("toolFeedbacks", convertToolFeedbacks(toolFeedbacks));
            response.put("message", "需要您的审批确认");

            return response;
        }

        // 没有触发工具调用，直接返回结果
        System.out.println("没有调用工具，直接返回");
        response.put("status", "success");
        response.put("messages", extractLastMessageText(result.orElse(null)));
        return response;
    }

    /**
     * 处理用户审批决策
     */
    @PostMapping("/sql/approval")
    public Map<String, Object> handleApproval(@RequestBody Map<String, Object> request) throws GraphRunnerException {
        Map<String, Object> response = new HashMap<>();

        String sessionId = (String) request.get("sessionId");
        Boolean approved = (Boolean) request.get("approved");

        if (sessionId == null) {
            response.put("status", "error");
            response.put("message", "sessionId 不能为空");
            return response;
        }

        PendingApproval pendingApproval = pendingApprovals.get(sessionId);
        if (pendingApproval == null) {
            response.put("status", "error");
            response.put("message", "未找到待审批的会话");
            return response;
        }


        // 用户批准，继续执行
        InterruptionMetadata interruptionMetadata = pendingApproval.interruptionMetadata;

        // 构建批准反馈
        InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
                .nodeId(interruptionMetadata.node())
                .state(interruptionMetadata.state());

        // 对所有工具调用设置批准决策
        interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
            InterruptionMetadata.ToolFeedback approvedFeedback =
                    InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                            .result(approved
                                    ? InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED // 通过
                                    : InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED) // 拒绝
                            .build();
            feedbackBuilder.addToolFeedback(approvedFeedback);
        });

        InterruptionMetadata approvalMetadata = feedbackBuilder.build();

        // 使用批准决策恢复执行
        RunnableConfig resumeConfig = RunnableConfig.builder()
                .threadId(sessionId)
                .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, approvalMetadata)
                .build();

        System.out.println("\n=== 用户已批准，继续执行 ===");
        Optional<NodeOutput> finalResult = reactAgent.invokeAndGetOutput("", resumeConfig);

        // 清除待审批状态
        pendingApprovals.remove(sessionId);

        if (finalResult.isPresent()) {
            System.out.println("执行完成，最终结果：" + finalResult.get());

            // 方案 1：尝试从 finalResult 中提取消息
            String messageText = extractLastMessageText(finalResult.get());
            if (messageText != null && !messageText.isEmpty()) {
                response.put("status", "approved");
                response.put("message", "SQL 已执行成功");
                response.put("resultMessage", messageText);
                return response;
            }

            // 方案 2：如果无法提取消息，返回通用成功消息
            response.put("status", "approved");
            response.put("message", "SQL 已执行成功");
            response.put("resultMessage", "✅ 操作已成功执行");

            return response;
        }

        response.put("status", "error");
        response.put("message", "执行失败");
        return response;
    }

    /**
     * 转换 ToolFeedback 为前端可用的格式
     */
    private List<Map<String, Object>> convertToolFeedbacks(List<InterruptionMetadata.ToolFeedback> toolFeedbacks) {
        return toolFeedbacks.stream().map(feedback -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", feedback.getName());
            map.put("arguments", feedback.getArguments());
            map.put("description", feedback.getDescription());
            return map;
        }).toList();
    }

    /**
     * 从 NodeOutput 中提取最后一条消息的文本
     */
    private String extractLastMessageText(NodeOutput nodeOutput) {
        if (nodeOutput == null) {
            return null;
        }
        try {
            // 尝试从 state 中获取 messages
            if (nodeOutput.state() != null) {
                Object messagesObj = nodeOutput.state().data().get("messages");
                if (messagesObj instanceof java.util.List) {
                    java.util.List<?> messages = (java.util.List<?>) messagesObj;
                    if (!messages.isEmpty()) {
                        Object lastMsg = messages.get(messages.size() - 1);
                        // 如果是 AssistantMessage，调用 getText() 方法
                        if (lastMsg instanceof org.springframework.ai.chat.messages.AssistantMessage) {
                            return ((org.springframework.ai.chat.messages.AssistantMessage) lastMsg).getText();
                        }
                        // 如果有 text 或 content 属性
                        if (lastMsg != null) {
                            try {
                                java.lang.reflect.Method getTextMethod = lastMsg.getClass().getMethod("getText");
                                return (String) getTextMethod.invoke(lastMsg);
                            } catch (Exception e) {
                                // 忽略反射错误，返回 null
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[extractLastMessageText] 提取消息失败：" + e.getMessage());
        }
        return null;
    }

}
