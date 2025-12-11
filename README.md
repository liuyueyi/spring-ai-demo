# spring-ai-demo

基于SpringAI的示例工程，快速体验SpringAI的能力，记录一下个人体验SpringAI进行大模型上层应用开发的学习历程，同时也给希望体验大模型应用开发的java开发者提供一些参考

## 技术栈

- SpringBoot 3.5.3
- SpringAI ~~1.0.1~~  🚀升级为 -> 1.1.2
- Java17
- Maven

## 教程目录

### 1.基础教程

主要介绍SpringAI的基础使用，对应的项目工程以 `Sxx-` 开头，通过这些实例，您将掌握SpringAI的基础知识（如提示词、上下文、架构化输出、tool calling, MCP, advise, ChatClient, 多模型等），并开始使用SpringAI进行大模型应用开发

- [x] [01.创建一个SpringAI-Demo工程.md](docs/01.创建一个SpringAI-Demo工程.md)
- [x] [02.提示词的使用.md](docs/02.提示词设置.md)
- [x] [03.结构化返回](docs/03.结构化返回.md)
- [x] [04.聊天上下文实现多轮对话](docs/04.聊天上下文.md)
- [x] [05.自定义大模型接入](docs/05.自定义大模型接入.md)
- [x] [06.Function Tool工具调用](docs/06.工具调用.md)
- [x] [07.实现一个简单的McpServer](docs/07.实现一个简单的McpServer.md)
- [x] [08.MCP Server简单鉴权的实现](docs/08.MCP%20Server简单鉴权的实现.md)]
- [x] [09.ChatClient使用说明](docs/09.ChatClient使用说明.md)]
- [x] [10.Advisor实现SpringAI交互增强](docs/10.Advisor实现SpringAI交互增强.md)]
- [x] [11.图像模型-生成图片](docs/11.图像模型.md)
- [x] [12.多模态实现食材图片卡路里识别示例](docs/12.多模态实现食材图片卡路里识别示例.md)
- [x] [13.支持MCP Client的AI对话实现](docs/13.支持MCP%20Client的AI对话实现.md)
- [x] [14.创建一个LangGraph4J示例工程](docs/14.创建一个Langgraph4j示例工程.md)
- [x] [15.接入OpenAI接口风格大模型](docs/15.接入OpenAI接口风格的大模型.md)
- [x] [16.异步流式模型调用](docs/16.异步流式模型调用.md)
- [x] [17.推理大模型接入与推理过程返回](docs/17.推理大模型接入与推理过程返回.md)
- [ ] [音频模型](docs/)
- [ ] [检索增强生成RAG](docs/)

### 2.进阶教程

进阶相关将主要介绍如何更好的使用SpringAI进行大模型应用开发，对应的实例工程都放在 [advance-projects](./advance-projects) 下

- [x] [01.使用MySql持久化对话历史](docs/A01.使用MySql持久化对话历史.md)
- [x] [02.使用H2持久化对话历史](docs/A02.使用H2持久化对话历史.md)]
- [x] [03.使用Redis持久化对话历史](docs/A03.使用Redis持久化对话历史.md)]
- [x] [04.使用LangGraph4J实现多伦对话](docs/A04.使用Langgraph4j实现多伦对话.md)
- [x] [05.使用LangGraph4J实现Agent路由选择](docs/A05.使用LangGraph4J实现Agent路由选择.md)

### 3.应用教程

以搭建完整可用的SpringAI应用为目的，演示SpringAI的业务边界和表现，对应项目工都放在 [app-projects](./app-projects) 下

- [x] [从0到1创建一个基于天气的旅游美食推荐智能体](docs/D01.从0到1创建一个基于天气的旅游美食推荐智能体.md) 

### 4.源码解读

以源码的视角，介绍SpringAI的核心实现，对应的项目工程以 `Yxx-` 开头