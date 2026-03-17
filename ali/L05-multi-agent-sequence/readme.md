# L05-multi-agent-sequence

- [多智能体实战 | 基于 Spring AI Alibaba 从0到1实现故事创作智能体](https://mp.weixin.qq.com/s/k_3viD7wDe616hhpiWvyrQ)


## 项目简介

本项目演示了如何使用 SequentialAgent 实现多智能体协作，完成文章创作任务。

## 功能特性

### 1. 多智能体序列

项目包含三个专业 Agent：
- **OutlineAgent**：大纲生成专家，负责根据主题生成文章大纲
- **WriterAgent**：专业作家，根据大纲撰写完整文章
- **ReviewAgent**：资深编辑，对文章进行评审和润色优化

### 2. 流式接口（新增）

提供基于 SSE (Server-Sent Events) 的流式 API 接口，实时返回创作进度：

#### 后端接口

```
GET /api/writer/stream?topic={文章主题}
```

**响应格式：**
```json
{
  "node": "节点名称",
  "agent": "智能体名称",
  "stage": "创作阶段 (outline/draft/review)",
  "content": "生成的内容",
  "hasContent": true
}
```

**技术特点：**
- 使用 `MediaType.TEXT_EVENT_STREAM_VALUE` 实现流式传输
- 实时推送每个 Agent 的创作进度
- 自动识别并标记创作阶段（大纲/初稿/优化）
- 完善的错误处理机制

#### 前端页面

访问地址：`/index`

**功能特性：**
- 三栏布局，分别展示大纲、初稿、最终稿
- 实时流式更新，可以看到文章生成的全过程
- 动态激活面板高亮当前创作阶段
- 响应式设计，支持不同屏幕尺寸
- 美观的渐变 UI 设计
- **Markdown 渲染支持**：自动解析 Markdown 格式，提供优美的排版效果
- **代码高亮**：支持多种编程语言的语法高亮（JavaScript、Java、Python、Bash、JSON 等）

**使用方式：**
1. 启动应用后，访问 `http://localhost:8080`
2. 在输入框中输入文章主题
3. 点击"开始创作"按钮
4. 实时观看三个智能体的协作过程

## 技术栈

- Spring Boot
- Spring AI
- Alibaba LangGraph4j
- Server-Sent Events (SSE)
- HTML5 + CSS3 + JavaScript

## 运行说明

1. 配置好 Spring AI 相关的环境变量（如 API Key）
2. 启动应用：`mvn spring-boot:run`
3. 访问前端页面或使用 API 接口

## 示例

### API 调用示例

```bash
curl -N http://localhost:8080/api/writer/stream?topic=春天的童话故事
```

### 前端访问

浏览器打开：`http://localhost:8080`

## 工作流程

```
用户输入主题 
  ↓
OutlineAgent → 生成大纲
  ↓
WriterAgent → 撰写初稿（基于大纲）
  ↓
ReviewAgent → 评审优化 → 最终文章
```

