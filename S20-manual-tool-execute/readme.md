# S20-manual-tool-execute

手动控制工具的执行

- 工具上下文: ToolContext
- 主动控制工具的执行:
  - 用户输入 -> LLM -> 工具消息 -> 本地执行工具，将工具结果给 -> LLM -> 总结最终输出内容
- 需要注意的时：工具名不要出现重复的情况，否则会出现报错

- [Spring AI工具调用如何对接真实业务？从自动到手动控制的完整链路剖析](https://mp.weixin.qq.com/s/TbTnpPkVPY_bTts_l8ltGQ)



**作者**：一灰  
**项目地址**：[https://github.com/liuyueyi/spring-ai-demo](https://github.com/liuyueyi/spring-ai-demo)  
**最后更新**：2026-03-06  
**标签**：#SpringAI #ReAct #Agent #Java #智能体编程
