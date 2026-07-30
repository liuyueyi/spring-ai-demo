# agent-scope

AgentScope 框架应用示例

### 第一阶段：新手村 —— 核心概念与快速起步

> **目标**：帮助 Java 开发者跑通环境，理解 AgentScope Java 2.0 的核心引擎。

- ✅ **[第1篇：简介与环境搭建](https://mp.weixin.qq.com/s/u4wERL0UXChPgvWQJpwurA)**：从零搭环境，跑通第一个 Agent 对话
- ✅ **[第2篇：ReActAgent 核心拆解](https://mp.weixin.qq.com/s/3SkybaLc47yu-EWOkd9G-w)**：理解"思考→行动→观察"的推理循环
- ✅ **[第3篇：你的第一个 Java Agent](https://mp.weixin.qq.com/s/l1HmsYK2w11U0hk-tadpLg)**：Builder 模式全面解析 + 流式输出 + 思考模式
- ✅ **[第4篇：工具系统（Tools）实战](https://mp.weixin.qq.com/s/3PPy1gB5gxhZSBSxWQ6rng)**：用 `@Tool` 注解将 Java 方法注册为 Agent 工具
- ✅ **[第5篇：结构化输出实战](https://mp.weixin.qq.com/s/92WrOmKjypAi4upDcTXy3g)** —— 让 Agent 直接返回 Java 对象

**第二阶段：进阶架构** —— 掌握 2.0 的核心亮点 HarnessAgent，理解"文件即配置"的工程化理念。

- **✅ [6.HarnessAgent：为 ReActAgent 装上操作系统]()**：对比 ReActAgent，理解 Harness 如何整合长期记忆、Session、子 Agent 编排与沙箱。
- **7. Workspace 驱动的人格与记忆**（下一篇）：通过 `AGENTS.md` 定义人设，通过 `MEMORY.md` 沉淀长期事实，实现"改文件即升级 Agent"。
- **8. Skills 技能系统（核心）**：掌握 `SKILL.md` 文件协议，理解"操作手册 + 资料包"的按需加载机制，节省 Token。
- **9. 多层级 SkillRepository 组合**：实战 Workspace、Git、Nacos、MySQL 等 5 层仓库的组合与隔离。
- **10. 流式事件与前端交互**：使用 `streamEvents()` 实时展示 Agent 执行过程，对接 Web UI 渲染。