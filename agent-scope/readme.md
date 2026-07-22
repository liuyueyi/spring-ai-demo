# agent-scope

AgentScope 框架应用示例

### 第一阶段：新手村 —— 核心概念与快速起步

> **目标**：帮助 Java 开发者跑通环境，理解 AgentScope Java 2.0 的核心引擎。

1. **AgentScope Java 2.0 简介与环境搭建**：从 GitHub 拉取源码编译安装，使用纯 Java 启动工程，接入 DeepSeek/OpenAI 兼容模型。
2. **核心推理引擎 ReActAgent**：理解“思考 → 调用工具 → 观察结果 → 继续思考”的核心循环机制。
3. **你的第一个 Java Agent**：使用 Builder 模式构建基础对话机器人，开启流式输出与思考模式。
4. **工具系统（Tools）实战**：通过 `@Tool` 和 `@ToolParam` 注解将任意 Java 方法注册为工具，让 LLM 自动调用。
5. **结构化输出**：让 Agent 直接返回指定的 Java 类型数据，告别繁琐的字符串解析。
