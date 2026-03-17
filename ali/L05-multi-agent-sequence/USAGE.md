# 文章创作流式接口使用说明

## 快速开始

### 1. 启动应用

确保已配置好 Spring AI 相关的环境变量（如 API Key），然后启动应用：

```bash
mvn spring-boot:run
```

### 2. 访问前端页面

在浏览器中打开：
```
http://localhost:8080
```

### 3. 使用步骤

1. 在输入框中输入文章主题，例如：
   - "春天的童话故事"
   - "人工智能的未来发展"
   - "如何学好编程"
   
2. 点击"开始创作"按钮

3. 实时观看三个智能体的协作过程：
   - 📋 **大纲设计**：OutlineAgent 生成文章大纲
   - ✍️ **初稿撰写**：WriterAgent 根据大纲撰写完整文章
   - ✨ **优化润色**：ReviewAgent 对文章进行评审和优化

## 技术实现

### 后端接口

**接口地址：** `GET /api/writer/stream?topic={主题}`

**响应类型：** `text/event-stream` (SSE)

**响应数据结构：**
```json
{
  "node": "节点名称",
  "agent": "智能体名称", 
  "stage": "创作阶段",
  "content": "生成的内容",
  "hasContent": true
}
```

**阶段标识：**
- `outline` - 大纲阶段
- `draft` - 初稿阶段
- `review` - 评审优化阶段

### 前端实现

**核心技术：**
- 使用 EventSource 接收 SSE 流式数据
- 三栏布局展示不同阶段的创作内容
- 动态累加内容，实时显示生成过程
- CSS 动画和渐变效果提供良好视觉体验

**关键特性：**
- 自动识别创作阶段并激活对应面板
- 内容累加器持续拼接流式返回的文本片段
- 错误处理和状态提示
- 响应式设计支持移动端

## API 调用示例

### cURL 命令

```bash
curl -N http://localhost:8080/api/writer/stream?topic=春天的童话故事
```

### JavaScript Fetch

```javascript
const eventSource = new EventSource('/api/writer/stream?topic=春天的童话故事');

eventSource.addEventListener('message', function(event) {
    const data = JSON.parse(event.data);
    console.log('收到数据:', data);
    
    if (data.stage === 'outline') {
        console.log('大纲内容:', data.content);
    } else if (data.stage === 'draft') {
        console.log('初稿内容:', data.content);
    } else if (data.stage === 'review') {
        console.log('优化后内容:', data.content);
    }
});

eventSource.addEventListener('error', function(event) {
    console.error('发生错误:', event.data);
});
```

## 工作流程详解

```
用户输入主题："春天的童话故事"
       ↓
┌─────────────────────────────┐
│  OutlineAgent (大纲生成)     │
│  输出：文章结构和章节大纲    │
└─────────────────────────────┘
       ↓
┌─────────────────────────────┐
│  WriterAgent (初稿撰写)      │
│  输入：大纲 + 主题           │
│  输出：完整的文章初稿        │
└─────────────────────────────┘
       ↓
┌─────────────────────────────┐
│  ReviewAgent (评审优化)      │
│  输入：初稿                  │
│  输出：优化润色后的最终文章  │
└─────────────────────────────┘
       ↓
返回给前端展示
```

## 常见问题

### Q: 为什么使用 SSE 而不是 WebSocket？

A: SSE 具有更好的简单性和原生 HTTP 兼容性，对于单向服务器推送场景（如本例）更加适合。

### Q: 前端页面卡顿怎么办？

A: 检查网络连接和服务器性能，确保 API Key 配置正确。

### Q: 可以自定义 Agent 吗？

A: 可以修改 `OutlineAgent.java`、`WriterAgent.java`、`ReviewAgent.java` 中的 Prompt 来调整各 Agent 的行为。

## 扩展建议

1. **添加更多 Agent**：可以在序列中添加专门的研究 Agent、事实核查 Agent 等
2. **支持中断和继续**：实现创作过程的中断和恢复功能
3. **历史记录**：添加创作历史保存和查看功能
4. **导出功能**：支持将最终文章导出为 Markdown、PDF 等格式
5. **多语言支持**：添加国际化支持

## 技术亮点

- ✅ 流式处理：实时返回创作进度，提升用户体验
- ✅ 阶段分离：清晰的三阶段划分，便于理解和调试
- ✅ 错误处理：完善的异常捕获和错误提示
- ✅ UI/UX 设计：美观的渐变界面和动态效果
- ✅ 响应式布局：适配不同设备屏幕
