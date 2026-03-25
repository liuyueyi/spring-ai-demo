//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.git.hui.springai.ali.context;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.MessageToolCallResultConverter;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class AgentTool {
    private static final ToolCallResultConverter CONVERTER = new MessageToolCallResultConverter();

    public AgentTool() {
    }

    public static ToolCallback create(ReactAgent agent) {
        Method method = ReflectionUtils.findMethod(AgentToolExecutor.class, "executeAgent", new Class[]{String.class, ToolContext.class});
        if (method == null) {
            throw new IllegalStateException("Could not find executeAgent method in AgentToolExecutor class");
        } else {
            // 使用反射访问 agent 的 inputSchema 和 inputType 字段
            String originalSchema = getInputSchemaFromAgent(agent);
            DefaultToolDefinition.Builder builder = ToolDefinitions
                    .builder(method)
                    .name(agent.name())
                    .description(agent.description());
            if (StringUtils.hasLength(originalSchema)) {
                String wrappedInputSchema = wrapSchemaInInputParameter(originalSchema);
                builder.inputSchema(wrappedInputSchema);
            }

            ToolDefinition toolDefinition = builder.build();
            AgentToolExecutor executor = new AgentToolExecutor(agent);
            return MethodToolCallback.builder().toolDefinition(toolDefinition).toolMethod(method).toolObject(executor).toolCallResultConverter(CONVERTER).build();
        }
    }

    /**
     * 使用反射从 ReactAgent 对象中获取 inputSchema 或 inputType
     */
    private static String getInputSchemaFromAgent(ReactAgent agent) {
        try {
            // 尝试获取 inputSchema 字段
            java.lang.reflect.Field schemaField = ReflectionUtils.findField(ReactAgent.class, "inputSchema");
            String schemaValue = null;
            if (schemaField != null) {
                schemaField.setAccessible(true);
                schemaValue = (String) schemaField.get(agent);
            }

            // 如果 schema 为空，尝试获取 inputType 并生成 schema
            if (!StringUtils.hasLength(schemaValue)) {
                java.lang.reflect.Field typeField = ReflectionUtils.findField(ReactAgent.class, "inputType");
                if (typeField != null) {
                    typeField.setAccessible(true);
                    Class<?> inputType = (Class<?>) typeField.get(agent);
                    if (inputType != null) {
                        schemaValue = JsonSchemaGenerator.generateForType(inputType, new JsonSchemaGenerator.SchemaOption[0]);
                    }
                }
            }

            return schemaValue;
        } catch (Exception e) {
            throw new RuntimeException("Failed to access agent input schema or type via reflection", e);
        }
    }

    private static String wrapSchemaInInputParameter(String originalSchema) {
        ObjectMapper objectMapper = JsonParser.getObjectMapper();

        try {
            Map<String, Object> originalSchemaMap = null;
            if (StringUtils.hasLength(originalSchema)) {
                try {
                    originalSchemaMap = (Map) objectMapper.readValue(originalSchema, new TypeReference<HashMap<String, Object>>() {
                    });
                } catch (Exception var5) {
                    originalSchemaMap = Map.of("type", "string");
                }
            } else {
                originalSchemaMap = Map.of("type", "string");
            }

            Map<String, Object> wrappedSchema = new HashMap();
            wrappedSchema.put("type", "object");
            Map<String, Object> properties = new HashMap();
            properties.put("input", originalSchemaMap);
            wrappedSchema.put("properties", properties);
            wrappedSchema.put("required", List.of("input"));
            return objectMapper.writeValueAsString(wrappedSchema);
        } catch (Exception var6) {
            return String.format("{\n\t\"type\": \"object\",\n\t\"properties\": {\n\t\t\"input\": {\n\t\t\t\"type\": \"string\"\n\t\t}\n\t},\n\t\"required\": [\"input\"]\n}\n");
        }
    }

    public static class AgentToolExecutor {
        private final ReactAgent agent;

        public AgentToolExecutor(ReactAgent agent) {
            this.agent = agent;
        }

        public AssistantMessage executeAgent(String input, ToolContext toolContext) {
            final String sessionId = (String) toolContext.getContext().get("sessionId");
            String actualInput = this.extractInputValue(input);

            List<Message> messagesToAdd = new ArrayList();
            if (StringUtils.hasLength(this.agent.instruction())) {
                messagesToAdd.add(AgentInstructionMessage.builder().text(this.agent.instruction()).build());
            }


            messagesToAdd.add(new UserMessage(actualInput));
            var graph = this.agent.getAndCompileGraph();

            // 使用流式调用
            Flux<NodeOutput> outputFlux = graph.stream(Map.of("messages", messagesToAdd));

//            Flux<NodeOutput> outputFlux;
//            try {
//                outputFlux = this.agent.stream(Map.of("input", actualInput));
//            } catch (GraphRunnerException e) {
//                throw new RuntimeException(e);
//            }
            NodeOutput lastOutput = outputFlux.map(nodeOutput -> {
                // 发送消息给前端页面
                String node = nodeOutput.node();
                String agentName = agent.name();

                log.info("【子Agent执行】收到节点输出 - node: {}, agent: {}", node, agentName);
                // 构建响应数据
                Map<String, Object> data = new HashMap<>();
                data.put("node", node);
                data.put("agent", agentName);

                StringBuilder contentBuilder = new StringBuilder();
                boolean hasContent = false;

                // 提取内容
                if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
                    Message message = streamingOutput.message();
                    log.info("节点类型 - node: {} type: {}", node, streamingOutput.getOutputType());

                    if (message instanceof AssistantMessage assistantMessage) {
                        // 检查是否有工具调用
                        if (assistantMessage.hasToolCalls()) {
                            // 工具调用完成
                            data.put("contentType", "tool_execute");
                            var toolCalls = assistantMessage.getToolCalls().get(0);
                            contentBuilder.append("\n[工具执行]：")
                                    .append(toolCalls.id())
                                    .append(":")
                                    .append(toolCalls.name())
                                    .append(" => args => ")
                                    .append(toolCalls.arguments());
                            hasContent = true;
                        } else {
                            // 流式消息，增量返回
                            String text = streamingOutput.message().getText();
                            if (text != null && !text.trim().isEmpty()) {
                                contentBuilder.append(text);
                                hasContent = true;
                                data.put("contentType", "delta");
                            }
                        }
                    } else if (message instanceof ToolResponseMessage) {
                        // 工具调用完成
                        data.put("contentType", "tool_complete");
                        contentBuilder.append("[工具执行完成]");
                        hasContent = true;
                    }
                }

                data.put("content", contentBuilder.toString());
                data.put("hasContent", hasContent);

                // 根据 agent 类型确定阶段标识
                String stage = determineStage(agentName);
                data.put("stage", stage);

                log.info("返回数据 - agent: {}, stage: {}, hasContent: {}, contentLength: {}", agentName, stage, hasContent, contentBuilder.length());

                String json = toJsonStr(data);

                // 如果 fluxSink 为空，尝试从 PlanContext 获取
                FluxSink contextSink = PlanContext.getSessionEmitter(sessionId);
                if (contextSink != null) {
                    contextSink.next(ServerSentEvent.<String>builder()
                            .event("message")
                            .data(json)
                            .build());
                }
                return nodeOutput;
            }).blockLast();
            OverAllState lastState = lastOutput != null ? lastOutput.state() : null;


            Optional<List> messages = Optional.ofNullable(lastState).flatMap((overAllState) -> {
                return overAllState.value("messages", List.class);
            });
            if (messages.isPresent()) {
                List<Message> messageList = (List) messages.get();
                return (AssistantMessage) messageList.get(messageList.size() - 1);
            } else {
                throw new RuntimeException("Failed to execute agent tool or failed to get agent tool result");
            }
        }

        private final ObjectMapper objectMapper = new ObjectMapper();

        private String toJsonStr(Object obj) {
            try {
                return objectMapper.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                log.error("错误信息 JSON 序列化失败", e);
                return "{\"error\":true,\"errorMessage\":\"JSON 序列化失败\"}";
            }
        }

        private String extractInputValue(String input) {
            if (!StringUtils.hasText(input)) {
                return input;
            } else {
                try {
                    ObjectMapper objectMapper = JsonParser.getObjectMapper();
                    Map<String, Object> jsonMap = (Map) objectMapper.readValue(input, new TypeReference<HashMap<String, Object>>() {
                    });
                    if (jsonMap != null && jsonMap.containsKey("input")) {
                        Object inputValue = jsonMap.get("input");
                        if (inputValue != null) {
                            if (inputValue instanceof String) {
                                return (String) inputValue;
                            }

                            return JsonParser.getObjectMapper().writeValueAsString(inputValue);
                        }
                    }
                } catch (Exception var5) {
                }

                return input;
            }
        }


        /**
         * 根据 agent 名称确定规划阶段
         */
        private String determineStage(String agentName) {
            if (agentName != null) {
                if (agentName.contains("attraction")) {
                    return "attraction"; // 景点推荐
                } else if (agentName.contains("hotel")) {
                    return "hotel"; // 住宿推荐
                } else if (agentName.contains("transport")) {
                    return "transport"; // 交通规划
                } else if (agentName.contains("supervisor")) {
                    return "supervisor"; // 监督者统筹
                }
            }
            return "unknown";
        }
    }
}
