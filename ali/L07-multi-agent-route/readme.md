# L07-multi-agent-route

基于 route 模式实现的智能客服，用于演示多智能体中的路由交接模式

在这种模式中，使用大语言模型（LLM）动态决定将请求路由到哪个子 Agent。

## 🎯 快速开始

### 1. 启动应用

确保已经配置好 API Key（在 `application.yml` 中设置 `silicon-api-key`），然后运行：

```bash
mvn spring-boot:run
```

### 2. 访问智能客服对话框

启动成功后，打开浏览器访问：

```
http://localhost:8080/
```

即可看到智能客服对话框界面。

## 💬 功能特性

- **流式实时响应**：采用 SSE (Server-Sent Events) 技术，实现打字机效果的实时对话体验
- **智能路由分发**：自动识别用户问题类型，无缝转接到对应的业务部门
- **可视化服务状态**：清晰展示当前为您服务的业务部门
- **多轮对话支持**：支持上下文理解，可以进行连续对话

### 测试示例问题：

1. **销售咨询**：
   - "你们的产品多少钱？"
   - "有什么优惠活动吗？"
   - "在哪里可以购买？"

2. **人力资源**：
   - "我想应聘软件工程师岗位"
   - "面试流程是怎样的？"
   - "什么时候发 offer？"

3. **技术支持**：
   - "系统登录不上去了怎么办？"
   - "这个功能怎么使用？"
   - "发现了一个 bug，如何反馈？"

## 🤖 智能体架构

这种router模式非常适合需要智能选择不同专家Agent的场景，比如我们这个示例中的 智能客服，将由下面几个智能体组成：

| Agent名称 | 角色描述 | outputKey | 
| --- | --- | --- | 
| router_agent | 路由决策者，根据用户意图选择专家 | （不产生输出，仅路由） | 
| tech_support_agent | 技术支持专家，处理产品使用/故障问题 | tech_response | 
| sales_agent | 销售专家，处理购买、价格等问题 | sales_response | 
| hr_agent | 招聘专家，处理招聘、面试等问题 | hr_response |


### 提示词（Instruction）

- router_agent 的 systemPrompt（用于定义路由规则）：

```
你是一个智能客服路由助手，你的职责是根据用户输入的内容，判断应该将问题转交给哪个专家Agent。
可选的专家有：
- tech_support_agent：处理产品故障、使用指导、技术咨询等问题。
- sales_agent：处理产品价格、购买渠道、优惠活动等销售相关问题。
- hr_agent：处理招聘信息、投递简历、面试安排等人力资源问题。

请只输出专家Agent的名称（如 tech_support_agent），不要输出其他任何内容。
```

若使用 LlmRoutingAgent，只需为每个子Agent提供清晰的 description：

tech_support_agent.description: “处理产品故障、使用指导、技术咨询等问题”

sales_agent.description: “处理产品价格、购买渠道、优惠活动等销售相关问题”

hr_agent.description: “处理招聘信息、投递简历、面试安排等人力资源问题”

然后由 LLM 自动匹配。

## 🔧 技术实现

### 后端接口

**流式对话接口**：`GET /api/cs/chat?message={用户消息}`

- **协议**：SSE (Server-Sent Events)
- **响应类型**：`text/event-stream`
- **返回格式**：JSON

**响应数据结构**：

```json
{
  "node": "节点名称",
  "agent": "当前处理的 agent",
  "content": "回复内容",
  "hasContent": true,
  "department": "sales|hr|tech|router"
}
```

### 前端特性

- 现代化的渐变 UI 设计
- 实时显示正在输入的动画效果
- 不同业务部门的视觉标识（颜色区分）
- 当前服务 Agent 的状态展示
- 完整的错误处理和用户反馈

## 📁 项目结构

```
src/main/java/com/git/hui/springai/ali/
├── controller/
│   └── CsController.java          # 智能客服 REST 控制器（流式接口）
├── cs/
│   ├── CsRouterAgent.java         # 路由 Agent 配置
│   ├── SalesAgent.java            # 销售 Agent
│   ├── HrAgent.java               # 人力资源 Agent
│   └── TechSupportAgent.java      # 技术支持 Agent
└── L07Application.java            # Spring Boot 启动类

src/main/resources/
├── templates/
│   └── cs-chat.html               # 智能客服对话框页面
└── application.yml                # 应用配置文件
```

## 🎨 界面预览

访问 `http://localhost:8080/` 后，您将看到一个现代化的智能客服对话框：

- **顶部**：显示客服状态（在线/离线）
- **中部**：四个业务部门的实时状态指示器
- **聊天区**：展示对话历史，包含部门标识
- **底部**：消息输入框和发送按钮

## ⚠️ 注意事项

1. 确保在 `application.yml` 中配置正确的 API Key
2. 需要网络连接以调用大模型服务
3. 建议使用 Chrome、Edge 等现代浏览器以获得最佳体验
4. 如果遇到问题，请查看控制台日志获取详细错误信息