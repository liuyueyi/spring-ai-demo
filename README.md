# spring-ai-demo

基于SpringAI的示例工程，快速体验SpringAI的能力，记录一下个人体验SpringAI进行大模型上层应用开发的学习历程，同时也给希望体验大模型应用开发的java开发者提供一些参考

项目关联主站：[https://ppai.top](https://ppai.top)

## 技术栈

- SpringBoot 3.5.3
- SpringAI ~~1.0.1~~  🚀升级为 -> 1.1.2
- LangGraph4J
- Java17
- Maven

v2 对应的目录下，存放的是基于 SpringAI 2.x 的示例工程

- Java21
- SpringBoot 4.x
- SpringAI 2.x

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
- [x] [18.语音模型之语音识别](https://www.ppai.top//ai-guides/ai-dev/基础篇/18.语音模型之语音识别.html#_1-初始化)
- [ ] [音频模型](docs/)
- [ ] [检索增强生成RAG](docs/)

### 2.进阶教程

进阶相关将主要介绍如何更好的使用SpringAI进行大模型应用开发，对应的实例工程都放在 [advance-projects](./advance-projects) 下

- [x] [01.使用MySql持久化对话历史](docs/A01.使用MySql持久化对话历史.md)
- [x] [02.使用H2持久化对话历史](docs/A02.使用H2持久化对话历史.md)]
- [x] [03.使用Redis持久化对话历史](docs/A03.使用Redis持久化对话历史.md)]
- [x] [04.使用LangGraph4J实现多伦对话](docs/A04.使用Langgraph4j实现多伦对话.md)
- [x] [05.使用LangGraph4J实现Agent路由选择](docs/A05.使用LangGraph4J实现Agent路由选择.md)
- [x] [06.告别传统AI开发！SpringAI Agent + Skills重新定义智能应用](https://mp.weixin.qq.com/s/ujxVleNhjxzUgL-rjfFcVA)
- [x] [07.Spring AI中的多轮对话艺术：让大模型主动提问获取明确需求](https://mp.weixin.qq.com/s/LcvmiIERs6aOIlRAKGGnFg)

### 3.应用教程

以搭建完整可用的SpringAI应用为目的，演示SpringAI的业务边界和表现，对应项目工都放在 [app-projects](./app-projects) 下

- [x] [从0到1创建一个基于天气的旅游美食推荐智能体](docs/D01.从0到1创建一个基于天气的旅游美食推荐智能体.md) 
- [x] [大模型应用开发实战：两百行实现一个自然语言地址提取智能体](https://mp.weixin.qq.com/s/96rHyp_gBUgmA2dhSbzNww)
- [x] [再见，OCR模板！你好，发票智能体：基于SpringAI与大模型的零配置发票智能提取架构](https://mp.weixin.qq.com/s/SnXdTB6tYqAzG7HgbnTSAQ)
- [x] [实战 | 零基础搭建知识库问答机器人：基于SpringAI+RAG的完整实现](https://mp.weixin.qq.com/s/NHqLJbos-_nrxNNmhg7IBQ)
- [x] [我用SpringAI造了个「微信红包封面设计师」](https://mp.weixin.qq.com/s/QyuWZ4EZ32pbcWn3fVphHQ)


对应的应用示例

| RAG问答机器人 | 微信红包封面设计Agent                | 
|----------|------------------------------|
| ![](https://ppai.top/ai-guides/imgs/column/springai/D04-3.gif)  | ![](./docs/static/T03-1.gif) |

### 4.源码解读

以源码的视角，介绍SpringAI的核心实现，对应的项目工程以 `Yxx-` 开头


### 5.LLM应用开发入门

- [LLM 应用开发是什么：零基础也可以读懂的科普文(极简版)](https://mp.weixin.qq.com/s/qCn8x2XO2shA8MheYbHq0w)
- [大模型应用开发系列教程：序-为什么你“会用 LLM”，但做不出复杂应用？](https://mp.weixin.qq.com/s/2GXBNOUq3jlysipftz8TpA)
- [大模型应用开发系列教程：第一章 LLM到底在做什么？](https://mp.weixin.qq.com/s/v-z6EHY300ElOxdGPdzc0w)
- [大模型应用开发系列教程：第二章 模型不是重点，参数才是你真正的控制面板](https://mp.weixin.qq.com/s/t_BuAW9i0npcaJdua3Am2Q)
- [大模型应用开发系列教程：第三章 为什么我的Prompt表现很糟？](https://mp.weixin.qq.com/s/vzt0bGwcfnASOiBa0Kc7VQ)
- [大模型应用开发系列教程：第四章 Prompt 的工程化结构设计](https://mp.weixin.qq.com/s/Nk-N34TLJVCTI5F4k5rGaQ)
- [大模型应用开发系列教程：第五章 从 Prompt 到 Prompt 模板与工程治理](https://mp.weixin.qq.com/s/ZQbztqBq7_PzynG06N4-mg)
- [大模型应用开发系列教程：第六章 上下文窗口的真实边界](https://mp.weixin.qq.com/s/nnKspRO87xbrn4-LBV3RNA)
- [大模型应用开发系列教程：第七章 从 “堆上下文” 到 “管理上下文”](https://mp.weixin.qq.com/s/_5D2tF6CPnafj5mlmlwLNw)
- [大模型应用开发系列教程：第八章 记忆策略的工程化选择](https://mp.weixin.qq.com/s/z5qaLtjChsvjhWNs8Nw05Q)

### 6. LangChain相关

- [LangChain开发入门系列](https://liuyueyi.github.io/langchain-demo/#)