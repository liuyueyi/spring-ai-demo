## D06-ai-auto-chat

author: [一灰灰](https://www.hhui.top/)

本项目实现一个 AI 对话系统，支持流式响应和结构化数据返回

### 功能特性

1. **流式响应**: 后端通过 SSE (Server-Sent Events) 实时推送 AI 回复内容
2. **多种响应类型**:
   - `text`: 纯文本
   - `card`: 卡片结构（标题、副标题、描述、图片、按钮等）
   - `list`: 列表结构（多个列表项）
   - `options`: 选项结构（可点击的选项按钮）
3. **多轮对话**: 支持会话 ID，实现上下文关联的多轮对话
4. **前端界面**: 提供美观的聊天界面，支持不同响应类型的展示

### 技术栈

- Spring Boot 3.5.4
- Spring AI 1.1.2
- 智谱 AI 大模型 (ZhiPuAi)
- Reactor (响应式编程)
- Thymeleaf (前端页面)

### 快速开始

#### 1. 配置 API Key

在 `src/main/resources/application.yml` 中设置智谱 AI 的 API Key：

```yaml
spring:
  ai:
    zhipuai:
      api-key: ${zhipuai-api-key}  # 替换为你的 API Key
      chat:
        options:
          model: GLM-4-Flash
```

或者通过启动参数设置：
```bash
mvn spring-boot:run -Dzhipuai-api-key=your-api-key-here
```

#### 2. 启动应用

```bash
cd app-projects/D06-ai-auto-chat
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动

#### 3. 访问聊天界面

打开浏览器访问：`http://localhost:8080/index.html`

### API 接口说明

#### 1. POST /api/chat/stream (流式聊天)

**请求:**
```http
POST /api/chat/stream
Content-Type: application/json
Accept: text/event-stream

{
  "message": "用户消息",
  "conversationId": "会话 ID(可选)",
  "responseType": "text|card|list|options"
}
```

**响应:** SSE 流，每个事件返回一个 JSON
```
data: {"type":"text","content":"部","conversationId":"xxx","done":false}
data: {"type":"text","content":"分","conversationId":"xxx","done":false}
data: {"type":"text","content":"","conversationId":"xxx","done":true}
```

#### 2. POST /api/chat (普通聊天)

**请求:**
```http
POST /api/chat
Content-Type: application/json

{
  "message": "用户消息",
  "conversationId": "会话 ID(可选)",
  "responseType": "text|card|list|options"
}
```

**响应:**
```json
{
  "type": "text",
  "content": "AI 回复的内容",
  "conversationId": "xxx",
  "done": true
}
```

#### 3. GET /api/chat/stream (GET 方式流式聊天)

方便测试的 GET 接口：

```http
GET /api/chat/stream?message=xxx&conversationId=xxx&responseType=text
```

### 响应类型示例

#### 1. 文本响应 (text)

```json
{
  "type": "text",
  "content": "这是一个普通的文本回复"
}
```

#### 2. 卡片响应 (card)

```json
{
  "type": "card",
  "data": {
    "title": "卡片标题",
    "subtitle": "副标题",
    "description": "卡片的详细描述内容",
    "imageUrl": "https://example.com/image.jpg",
    "actions": [
      {
        "text": "按钮 1",
        "actionValue": "action1",
        "actionType": "primary"
      }
    ]
  }
}
```

#### 3. 列表响应 (list)

```json
{
  "type": "list",
  "data": [
    {
      "title": "列表项 1",
      "description": "列表项 1 的描述",
      "iconUrl": "https://example.com/icon1.jpg"
    },
    {
      "title": "列表项 2",
      "description": "列表项 2 的描述"
    }
  ]
}
```

#### 4. 选项响应 (options)

```json
{
  "type": "options",
  "content": "请选择您感兴趣的话题：",
  "data": [
    {
      "label": "选项 A",
      "value": "option_a",
      "description": "选项 A 的描述"
    },
    {
      "label": "选项 B",
      "value": "option_b",
      "description": "选项 B 的描述"
    }
  ]
}
```

### 使用示例

#### 请求文本回复
```
消息：今天天气怎么样？
响应类型：text
```

#### 请求卡片回复
```
消息：推荐一道川菜
响应类型：card
```

AI 会返回类似这样的结构化数据：
```json
{
  "title": "麻婆豆腐",
  "subtitle": "经典川菜代表",
  "description": "麻婆豆腐是四川省传统名菜，主要原料为豆腐和牛肉末...",
  "imageUrl": "https://example.com/mapo-tofu.jpg"
}
```

#### 请求列表回复
```
消息：推荐几本好看的科幻小说
响应类型：list
```

#### 请求选项回复
```
消息：我想学习编程，应该从哪种语言开始？
响应类型：options
```

AI 会返回几个选项供用户选择。

### 项目结构

```
D06-ai-auto-chat/
├── src/main/
│   ├── java/com/git/hui/springai/app/
│   │   ├── dto/
│   │   │   ├── ChatRequest.java      # 请求 DTO
│   │   │   └── ChatResponse.java     # 响应 DTO
│   │   ├── mvc/
│   │   │   └── SimpleChatController.java   # 控制器
│   │   ├── service/
│   │   │   └── ChatService.java      # 服务层
│   │   └── D06Application.java                 # 启动类
│   └── resources/
│       ├── application.yml           # 配置文件
│       └── templates/simpleChat.html         # 前端页面
└── pom.xml                           # Maven 配置
```

### 注意事项

1. **API Key 安全**: 不要将 API Key 提交到版本控制系统，建议使用环境变量或配置文件占位符
2. **会话管理**: 当前使用内存存储会话历史，生产环境建议使用 Redis 等持久化方案
3. **跨域支持**: 已配置允许所有来源的跨域请求，可根据需要调整
4. **错误处理**: 包含基本的错误处理机制，可根据业务需求扩展

### 扩展建议

1. **添加更多响应类型**: 如图表、表格、时间线等
2. **集成工具调用**: 结合 Function Calling 实现更丰富的功能
3. **RAG 支持**: 集成知识库实现基于检索的增强生成
4. **多模态**: 支持图片、音频等多媒体输入输出
5. **鉴权机制**: 添加用户认证和授权

### 参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [SSE 规范](https://html.spec.whatwg.org/multipage/server-sent-events.html)
