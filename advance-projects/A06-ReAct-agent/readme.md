# A06-ReAct-Agent - 基于 SpringAI 1.1.2 的 ReAct 模式实现

- [深入理解 ReAct 模式：基于Spring AI从0到1实现一个ReAct Agent](https://mp.weixin.qq.com/s/mJIibMdAFSDgXZBsM3tuPw)

## 一、ReAct 模式简介

ReAct (Reasoning + Acting) 是一种将推理和行动结合的大模型代理模式。其核心思想是让大模型在解决问题时，能够:

1. **Thinking(思考)**: 分析当前情况，决定下一步该做什么
2. **Act(行动)**: 调用工具执行具体操作
3. **Observe(观察)**: 观察工具执行结果，获取反馈

然后循环这个过程，直到问题解决。

## 二、项目结构

```
A06-ReAct-Agent/
├── src/main/java/com/git/hui/springai/app/
│   ├── react/
│   │   ├── ReActAgent.java           # ReAct Agent 基类
│   │   ├── ToolReActAgent.java       # 带工具的 ReAct Agent 示例
│   │   ├── ReActAgentRunner.java     # 测试运行器
│   │   └── LlmService.java           # 大模型服务封装
│   └── advisor/
│       └── MyLoggingAdvisor.java     # 日志记录 Advisor
└── pom.xml
```
