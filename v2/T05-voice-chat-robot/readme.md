# T05-voice-chat-robot 语音聊天机器人

这是一个基于Spring AI的语音聊天机器人项目，支持音频文件上传和实时麦克风录音的语音识别与翻译功能。

对应的技术博文，请参照： [实战干货！Spring AI 集成语音识别，实现实时翻译机器人的完整指南](https://mp.weixin.qq.com/s/qF0RfLts-fuMzv-uJZnBig)


## 功能特性

- 🎤 实时麦克风录音功能
- 📁 音频文件上传识别
- 🌍 多语言语音识别与翻译
- 🔁 异步处理机制
- ⚡ **并行音频分割处理**（新增）
- 📊 **智能分割策略**（新增）
- 💾 翻译历史记录
- 🎵 音频播放与下载

## 技术架构

### 核心组件

1. **WebViewController** - 页面视图控制器
2. **AutoTranslateController** - 自动翻译控制器（核心）
3. **AudioTransactionService** - 音频转录服务
4. **AudioSegmentationService** - 音频分割服务（新增）
5. **前端页面** - up.html 和 translate.html

### 依赖技术栈

- Spring Boot 3.x
- Spring AI OpenAI Starter
- Thymeleaf 模板引擎
- Apache HttpClient
- Web Speech API (前端)

## 系统时序图

### 简化版核心流程图

```mermaid
flowchart TD
    A[用户上传音频] --> B[生成任务ID]
    B --> C[返回任务ID给前端]
    C --> D[前端建立SSE连接]
    D --> E[后端异步处理]
    
    subgraph "并行处理"
        E --> F[音频转录]
        E --> G[文本翻译]
    end
    
    F --> H[推送转录结果]
    G --> I[流式推送翻译]
    I --> J[处理完成]
    
    H --> K[前端实时显示]
    I --> K
    J --> K
    
    K --> L[用户获得最终结果]
    
    style A fill:#e1f5fe
    style B fill:#f3e5f5
    style E fill:#fff3e0
    style F fill:#e8f5e8
    style G fill:#e8f5e8
    style K fill:#ffebee
```

### 完整版时序图

下面是完整的用户交互流程时序图，展示了从用户打开页面到获得最终翻译结果的全过程：

```mermaid
sequenceDiagram
    participant U as 用户(User)
    participant B as 浏览器(Browser)
    participant WC as WebViewController
    participant ATC as AutoTranslateController
    participant ATS as AudioTransactionService
    participant SF as SiliconFlow API
    participant CM as ChatModel
    
    Note over U,SF: 用户上传音频文件场景
    
    U->>B: 1. 打开翻译页面(/translate)
    B->>ATC: 2. GET /auto/translate 页面请求
    ATC-->>B: 3. 返回 translate.html 页面
    
    U->>B: 4. 选择音频文件或开始录音
    Note right of B: 用户可以选择:<br/>- 上传已有音频文件<br/>- 使用麦克风实时录音
    
    B->>ATC: 5. POST /auto/uploadAudio<br/>(multipart/form-data)
    Note right of ATC: 接收音频文件参数:<br/>- file: 音频文件<br/>- targetLanguage: 目标语言
    
    ATC->>ATC: 6. 生成唯一任务ID(UUID)
    ATC->>ATC: 7. 创建TranslationTask记录
    ATC->>ATC: 8. 存储到taskMap内存映射
    
    ATC-->>B: 9. 返回 {taskId: "uuid-string"}
    
    B->>B: 10. 基于taskId建立SSE连接
    B->>ATC: 11. GET /auto/getResultStream/{taskId}
    
    ATC->>ATC: 12. 存储SseEmitter到emitters映射
    ATC-->>B: 13. 保持SSE连接开放
    
    Note over ATC: 异步处理开始
    
    par 并行处理音频转录
        ATC->>ATS: 14. 调用audioTransaction(file)
        ATS->>SF: 15. POST /v1/audio/transcriptions<br/>(音频文件 + 模型参数)
        SF-->>ATS: 16. 返回转录文本结果
        ATS-->>ATC: 17. 返回识别后的文本内容
        
        ATC->>ATC: 18. 等待SSE连接建立
        loop 等待SSE连接
            ATC->>ATC: 检查emitters.containsKey(taskId)
            Note right of ATC: 轮询等待前端建立SSE连接
        end
        
        ATC->>B: 19. SSE推送转录结果<br/>event: "transcription"<br/>data: "识别的文本内容"
    and 并行处理翻译
        ATC->>CM: 20. 调用ChatClient流式翻译<br/>Prompt包含:<br/>- 系统提示词<br/>- 源文本<br/>- 目标语言
        CM->>CM: 21. 流式生成翻译结果
        loop 翻译流式输出
            CM->>ATC: 22. 推送翻译片段
            ATC->>B: 23. SSE推送翻译结果<br/>event: "translation"<br/>data: "翻译文本片段"
        end
        CM->>ATC: 24. 翻译完成信号
        ATC->>B: 25. SSE推送结束信号<br/>event: "end"<br/>data: "true"
    end
    
    Note over B: 前端实时显示处理进度
    
    B->>B: 26. 实时更新UI显示:<br/>- 音频转录结果<br/>- 流式翻译内容<br/>- 处理状态
    
    ATC->>ATC: 27. 清理任务资源<br/>- 从taskMap移除任务<br/>- 从emitters移除连接<br/>- 关闭SSE连接
    
    Note over U,B: 用户获得最终结果
```

## 系统设计方案

### 🎯 设计理念

本系统采用**异步非阻塞**的处理架构，核心目标是：
- **用户体验优先**：避免用户长时间等待
- **资源高效利用**：并行处理多个任务
- **实时反馈机制**：通过SSE提供处理进度

### 🏗️ 整体架构设计

```mermaid
graph TB
    A[前端用户界面] --> B[Web服务器]
    B --> C[任务调度中心]
    C --> D[音频转录服务]
    C --> E[文本翻译服务]
    D --> F[AI识别引擎]
    E --> G[AI翻译引擎]
    F --> H[结果缓存]
    G --> H
    H --> I[SSE实时推送]
    I --> A
    
    style A fill:#e3f2fd
    style C fill:#f3e5f5
    style D fill:#e8f5e8
    style E fill:#e8f5e8
    style H fill:#fff3e0
```

### 📋 核心设计要点

#### 1. 异步任务处理机制

**为什么采用异步？**
- 音频处理耗时较长（通常几秒到几十秒）
- 同步等待会让用户界面卡顿
- 用户体验差，容易误认为系统无响应

**解决方案：**
```
用户上传音频
    ↓
立即返回任务ID（100ms内）
    ↓
后台异步处理
    ↓
实时推送处理进度
    ↓
用户获得最终结果
```

#### 2. 并行处理优化

**双任务并行执行：**
- ✅ 音频转录（调用SiliconFlow API）
- ✅ 文本翻译（调用ChatModel）

**优势：**
- 总处理时间 ≈ Max(转录时间, 翻译时间)
- 而不是 转录时间 + 翻译时间
- 效率提升约40-60%

#### 3. 实时通信设计

**SSE (Server-Sent Events) 选择理由：**
- 🔸 单向推送，服务端主动
- 🔸 HTTP协议，兼容性好
- 🔸 自动重连机制
- 🔸 比WebSocket轻量

**推送时机设计：**
```
转录完成 → 立即推送识别文本
翻译进行中 → 流式推送翻译片段
翻译完成 → 发送结束信号
```

## API 接口说明

### 核心接口

1. **上传音频并开始处理**
   ```
   POST /auto/uploadAudio
   Content-Type: multipart/form-data
   
   参数:
   - file: 音频文件
   - targetLanguage: 目标翻译语言(可选，默认英语)
   
   返回:
   {
     "taskId": "uuid-string"
   }
   ```

2. **获取处理结果流**
   ```
   GET /auto/getResultStream/{taskId}
   Accept: text/event-stream
   
   SSE事件类型:
   - transcription: 音频转录结果
   - translation: 翻译结果片段  
   - end: 处理完成标志
   ```

3. **同步翻译接口（支持并行处理）**
   ```
   POST /auto/autoTranslateSync
   Content-Type: multipart/form-data
   
   参数:
   - file: 音频文件
   - targetLanguage: 目标语言（可选）
   - useParallel: 是否使用并行处理（可选，默认false）
   
   返回: 最终翻译结果文本
   ```

4. **单线程音频转录接口**
   ```
   POST /translateAudio
   Content-Type: multipart/form-data
   
   参数:
   - file: 音频文件
   
   返回: 转录结果文本
   ```

5. **并行处理音频转录接口**
   ```
   POST /translateAudioParallel
   Content-Type: multipart/form-data
   
   参数:
   - file: 音频文件
   - useParallel: 是否使用并行处理（必填）
   
   返回: 转录结果文本
   ```

### 🚀 并行处理使用建议

- **适用场景**：音频文件大于10MB或时长大于1分钟
- **性能收益**：大文件处理速度提升40-80%
- **资源消耗**：会占用更多CPU和网络资源
- **错误处理**：并行处理失败时自动回退到单线程处理

**推荐使用方式**：
```javascript
// 对于大文件启用并行处理
const formData = new FormData();
formData.append('file', audioFile);
formData.append('useParallel', 'true');

fetch('/translateAudioParallel', {
    method: 'POST',
    body: formData
})
.then(response => response.text())
.then(result => {
    console.log('转录结果:', result);
});
```

## 配置要求

### 环境配置
- JDK 17+
- Maven 3.8+
- SiliconFlow API Key

### application.yml 配置示例
```yaml
spring:
  ai:
    openai:
      api-key: your_siliconflow_api_key
      base-url: https://api.siliconflow.cn/v1
```

## 部署运行

```bash
# 编译项目
mvn clean package

# 运行应用
java -jar target/T05-voice-chat-robot-1.0.0-SNAPSHOT.jar

# 访问地址
http://localhost:8080/translate
```

## 注意事项

1. **浏览器兼容性**：录音功能需要现代浏览器支持 Web Audio API
2. **文件大小限制**：建议音频文件不超过100MB
3. **网络要求**：需要能够访问 SiliconFlow API
4. **并发处理**：系统使用内存存储任务状态，重启会丢失未完成任务

## 项目结构

```
src/main/
├── java/com/git/hui/springai/app/
│   ├── controller/
│   │   ├── AutoTranslateController.java    # 核心翻译控制器
│   │   └── WebViewController.java          # 页面控制器
│   ├── service/
│   │   └── AudioTransactionService.java    # 音频转录服务
│   └── T05Application.java                 # 应用启动类
└── resources/
    ├── templates/
    │   ├── translate.html                  # 翻译主页面
    │   └── up.html                         # 音频上传页面
    └── application.yml                     # 配置文件
```