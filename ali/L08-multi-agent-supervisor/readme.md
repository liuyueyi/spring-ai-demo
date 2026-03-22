# L08-multi-agent-supervisor - 旅游行程规划智能体

## 📖 项目介绍

这是一个基于Spring AI Alibaba 的 Supervisor Agent 实现多智能体交接的示例工程，主要实现的是一个旅游行程规划智能体系统。

该系统通过一个监督者（Supervisor）协调多个专家 Agent 共同完成旅行计划的制定：

- **🏛️ 景点推荐专家（AttractionAgent）**：负责推荐景点、安排游览路线
- **🏨 住宿推荐专家（HotelAgent）**：负责推荐酒店、民宿等住宿
- **🚗 交通规划专家（TransportAgent）**：负责规划城市间交通和市内交通
- **📋 监督者（TravelSupervisorPlanAgent）**：协调各专家工作，整合最终方案

## 🎯 功能特性

- ✅ **流式 SSE 接口**：实时返回规划进度，提升用户体验
- ✅ **多智能体协作**：自动协调多个专家 Agent 完成复杂任务
- ✅ **现代化 UI**：美观的渐变界面和动态效果
- ✅ **Markdown 支持**：支持 Markdown 格式的输出展示
- ✅ **错误处理**：完善的异常捕获和错误提示

## 🚀 快速开始

### 1. 配置 API Key

在 `src/main/resources/application.yml` 中配置你的 API Key：

```yaml
spring:
  ai:
    openai:
      api-key: ${silicon-api-key}  # 替换为你自己的 API Key
      base-url: https://api.siliconflow.cn
```

或者通过启动参数设置：
```bash
mvn spring-boot:run -Dsilicon-api-key=your-api-key-here
```

### 2. 启动应用

```bash
cd L08-multi-agent-supervisor
mvn clean install
mvn spring-boot:run
```

### 3. 访问前端页面

打开浏览器访问：
```
http://localhost:8080/travel-plan.html
```

## 🔧 后端接口

### 流式旅游规划接口

**接口地址**：`GET /api/travel/plan`

**请求参数**：
- `destination` (必填)：目的地，例如：北京、巴黎、东京
- `days` (可选)：旅行天数，默认 3 天
- `budget` (可选)：预算，例如：5000 元、$1000
- `preferences` (可选)：偏好，例如：美食、购物、自然风光

**响应类型**：`text/event-stream` (SSE)

**响应数据结构**：
```json
{
  "node": "节点名称",
  "agent": "当前处理的 agent",
  "content": "回复内容",
  "hasContent": true,
  "contentType": "delta|tool_complete",
  "stage": "attraction|hotel|transport|supervisor"
}
```

**示例请求**：
```bash
curl -X GET "http://localhost:8080/api/travel/plan?destination=北京&days=5&budget=5000 元&preferences=美食，历史文化"
```

## 📁 项目结构

```
src/main/java/com/git/hui/springai/ali/
├── controller/
│   └── TravelPlanController.java      # 旅游规划 REST 控制器（流式接口）
├── planer/
│   ├── TravelSupervisorPlanAgent.java # 监督者 Agent
│   ├── AttractionAgent.java           # 景点推荐 Agent
│   ├── HotelAgent.java                # 住宿推荐 Agent
│   └── TransportAgent.java            # 交通规划 Agent
└── L08Application.java                # Spring Boot 启动类

src/main/resources/
├── templates/
│   └── travel-plan.html               # 旅游规划前端页面
└── application.yml                    # 应用配置文件
```

## 💡 使用示例

### 前端使用

1. 打开页面后，在输入框中填写：
   - 目的地（必填）
   - 旅行天数（默认 3 天）
   - 预算（可选）
   - 偏好（可选）

2. 点击"开始规划"按钮

3. 系统会实时显示各专家的工作进度：
   - 🏛️ 景点专家推荐景点和游览路线
   - 🏨 酒店专家根据景点推荐住宿
   - 🚗 交通专家规划交通方式
   - 📋 监督者整合完整方案

4. 规划完成后，可以查看完整的旅行计划

### 技术实现细节

#### Supervisor Agent 工作流程

1. **接收用户请求**：监督者接收用户的旅行规划需求
2. **任务分解**：将复杂任务分解为景点、住宿、交通等子任务
3. **协调执行**：依次调用对应的专家 Agent 完成子任务
4. **结果整合**：汇总所有专家的建议，形成完整的旅行计划

#### 流式输出处理

```java
Flux<NodeOutput> agentStream = supervisorAgent.stream(prompt);

return agentStream
    .filter(nodeOutput -> !(nodeOutput instanceof StreamingOutput<?> so && 
            so.getOutputType() == OutputType.AGENT_MODEL_FINISHED))
    .map(nodeOutput -> {
        // 处理每个节点的输出
        String node = nodeOutput.node();
        String agentName = nodeOutput.agent();
        // ... 构建响应数据
    });
```

## 🎨 界面预览

访问页面后，您将看到一个现代化的旅游规划界面：

- **顶部**：系统标题和描述
- **输入区**：目的地、天数、预算、偏好输入框
- **规划区**：四个专家的实时工作面板
- **状态栏**：显示当前处理状态和活跃的专家

## ⚠️ 注意事项

1. **API Key 安全**：不要将 API Key 提交到版本控制系统
2. **网络连接**：需要稳定的网络连接才能访问 AI 服务
3. **模型选择**：可以在配置文件中切换不同的 AI 模型

## 🔍 常见问题

### Q: 为什么使用 SSE 而不是 WebSocket？

A: SSE 具有更好的简单性和原生 HTTP 兼容性，对于单向服务器推送场景更加适合。

### Q: 前端页面卡顿怎么办？

A: 检查网络连接和服务器性能，确保 API Key 配置正确。

### Q: 可以自定义 Agent 吗？

A: 可以修改各个 Agent 中的 Prompt 来调整它们的行为和输出风格。

## 📝 扩展建议

1. **添加更多专家**：可以添加餐饮推荐专家、购物指南专家等
2. **支持多轮对话**：实现基于上下文的连续对话规划
3. **保存历史记录**：添加旅行计划保存和分享功能
4. **导出功能**：支持将计划导出为 PDF、Word 等格式
5. **地图集成**：在地图上显示景点位置和路线

## 🌟 技术亮点

- ✅ **流式处理**：实时返回规划进度，提升用户体验
- ✅ **多 Agent 协作**：清晰的职责划分，便于维护和扩展
- ✅ **错误处理**：完善的异常捕获和错误提示
- ✅ **UI/UX 设计**：美观的渐变界面和动态效果
- ✅ **响应式布局**：适配不同设备屏幕

## 📄 License

本示例仅供学习和研究使用。
