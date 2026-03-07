### 3) HTTP 接口契约（iframe -> BFF）

#### 3.1 通用请求头

| Header | 必填 | 类型 | 说明 |
|---|---|---|---|
| `Content-Type` | 是 | string | 固定 `application/json` |
| `Authorization` | 建议是（严格模式必填） | string | `Bearer <token>` |
| `X-Copilot-App-Id` | 建议是（严格模式必填） | string | 应与 `context.appId` 一致 |
| `X-Copilot-Protocol-Version` | 建议是 | string | 当前版本 `2026-03-03` |
| `X-Request-Id` | 否 | string | 请求级唯一 ID，便于排障 |
| `X-Trace-Id` | 否 | string | 端到端链路追踪 ID |
| `X-Copilot-Session-Id` | 否 | string | 会话 ID（有会话时建议传） |
| `X-Idempotency-Key` | 否（`/tool/execute` 严格模式必填） | string | 工具执行幂等键 |

说明：
- 非严格模式下，部分头缺失不会直接报错；严格模式下会按协议校验。
- `Authorization` 格式错误时返回 `401 UNAUTHORIZED`。

#### 3.2 `POST /agent/chat`（流式 NDJSON）

请求体（`AgentChatRequest`）字段说明：

| 字段 | 必填 | 类型 | 说明 |
|---|---|---|---|
| `sessionId` | 否 | string | 会话 ID；不传则后端自动创建 |
| `message` | 是 | string | 用户输入文本，去前后空格后不能为空，长度 <= 4000 |
| `context` | 是 | object | Copilot 上下文 |
| `context.appId` | 建议是（严格模式必填） | string | 应与 `X-Copilot-App-Id` 一致 |
| `context.token` | 建议是 | string | 与鉴权 token 对齐 |
| `context.url` | 建议是 | string | 当前页面 URL |
| `context.title` | 建议是 | string | 页面标题 |
| `metadata` | 否 | object | 元数据 |
| `metadata.requestId` | 否 | string | 请求 ID（可覆盖 header requestId） |
| `metadata.resumeFrom` | 否 | object | 流恢复锚点 |
| `metadata.resumeFrom.sequence` | 否 | number | 上次处理到的 sequence |
| `metadata.resumeFrom.eventId` | 否 | string | 上次处理到的 eventId |

```ts
type AgentChatRequest = {
  sessionId?: string
  message: string
  context: CopilotContext
  metadata?: Record<string, unknown> // 常见: { requestId, resumeFrom }
}
```

返回：
- `200 OK`
- `Content-Type: application/x-ndjson`
- 按行返回事件对象（每行一个 JSON）：

```ts
type NdjsonEvent = {
  type: string
  payload?: Record<string, unknown>
  meta?: {
    traceId?: string
    requestId?: string
    sessionId?: string
    sequence?: number
    eventId?: string
    ts?: string
    protocolVersion?: string
    [key: string]: unknown
  }
}
```

前端终止判定（当前实现）：
- 收到 `session.end` 或
- 收到 `status.waiting_user` 或
- 收到 `error`

常见错误码（`/agent/chat`）：

| HTTP 状态码 | code | 触发场景 |
|---|---|---|
| `400` | `INVALID_HEADER` / `MISSING_REQUIRED_HEADER` | Header 不合法 |
| `401` | `UNAUTHORIZED` | `Authorization` 非 `Bearer <token>` |
| `409` | `PROTOCOL_VERSION_MISMATCH` / `APP_ID_MISMATCH` / `SESSION_APP_ID_MISMATCH` | 协议版本、appId 或会话归属不一致 |
| `413` | `REQUEST_BODY_TOO_LARGE` | 请求体超过限制 |
| `415` | `UNSUPPORTED_MEDIA_TYPE` | 非 JSON 请求 |
| `422` | `INVALID_MESSAGE` / `MESSAGE_TOO_LONG` / `MISSING_CONTEXT_APP_ID` | message/context 参数不合法 |
| `500` | `MOCK_SERVER_ERROR` | 服务端异常 |

#### 3.3 `POST /agent/tool/execute`

请求体（`AgentToolExecuteRequest`）字段说明：

| 字段 | 必填 | 类型 | 说明 |
|---|---|---|---|
| `sessionId` | 否 | string | 所属会话 ID |
| `toolCallId` | 否 | string | 工具调用 ID；不传会自动生成 |
| `action` | 是 | enum | `confirm` / `form_submit` / `ui_action` |
| `input` | 否 | object | 工具入参载荷 |
| `context` | 是 | object | Copilot 上下文（同 chat） |
| `context.appId` | 建议是（严格模式必填） | string | 应与 `X-Copilot-App-Id` 一致 |

```ts
type AgentToolExecuteRequest = {
  sessionId?: string
  toolCallId?: string
  action: 'confirm' | 'form_submit' | 'ui_action'
  input?: Record<string, unknown>
  context: CopilotContext
}
```

返回体（`AgentToolExecuteResponse`）：

| 字段 | 必填 | 类型 | 说明 |
|---|---|---|---|
| `ok` | 是 | boolean | 是否成功 |
| `toolCallId` | 否 | string | 工具调用 ID |
| `result` | 否 | object | 成功结果 |
| `error` | 否 | object | 失败详情 |
| `meta` | 否 | object | 协议元信息 |
| `idempotentReplay` | 否 | boolean | 是否命中幂等重放 |

```ts
type AgentToolExecuteResponse = {
  ok: boolean
  toolCallId?: string
  result?: Record<string, unknown>
  error?: {
    status?: number
    code: string
    message: string
    retryable?: boolean
    details?: unknown
  }
  meta?: Record<string, unknown>
  idempotentReplay?: boolean
}
```

常见错误码（`/agent/tool/execute`）：

| HTTP 状态码 | code | 触发场景 |
|---|---|---|
| `401` | `UNAUTHORIZED` | 鉴权头不合法 |
| `404` | `SESSION_NOT_FOUND` | `sessionId` 不存在或不属于当前 app |
| `409` | `APP_ID_MISMATCH` / `IDEMPOTENCY_CONFLICT` | appId 不一致 / 幂等键冲突 |
| `422` | `INVALID_TOOL_ACTION` / `MISSING_CONTEXT_APP_ID` | action 或 context 不合法 |
| `428` | `MISSING_IDEMPOTENCY_KEY` | 严格模式下缺幂等键 |

#### 3.4 `GET /agent/session/:id`

路径参数：

| 参数 | 必填 | 类型 | 说明 |
|---|---|---|---|
| `id` | 是 | string | 会话 ID |

返回体字段：

| 字段 | 必填 | 类型 | 说明 |
|---|---|---|---|
| `ok` | 否 | boolean | 成功标识 |
| `session.id` | 是 | string | 会话 ID |
| `session.title` | 是 | string | 会话标题 |
| `session.createdAt` | 是 | string | 创建时间（ISO） |
| `session.updatedAt` | 是 | string | 更新时间（ISO） |
| `session.status` | 是 | enum | `active` / `ended` |
| `meta` | 否 | object | 协议元信息 |

```ts
type AgentSessionResponse = {
  ok?: boolean
  session: {
    id: string
    title: string
    createdAt: string
    updatedAt: string
    status: 'active' | 'ended'
  }
  meta?: Record<string, unknown>
}
```

常见错误码：
- `404 SESSION_NOT_FOUND`

#### 3.5 `GET /agent/sessions`

查询参数：无

返回体字段：

| 字段 | 必填 | 类型 | 说明 |
|---|---|---|---|
| `ok` | 否 | boolean | 成功标识 |
| `items` | 是 | array | 会话列表（按 `updatedAt` 倒序） |
| `items[].id` | 是 | string | 会话 ID |
| `items[].title` | 是 | string | 会话标题 |
| `items[].createdAt` | 是 | string | 创建时间（ISO） |
| `items[].updatedAt` | 是 | string | 更新时间（ISO） |
| `items[].status` | 是 | enum | `active` / `ended` |
| `meta` | 否 | object | 协议元信息 |

```ts
type AgentSessionsResponse = {
  ok?: boolean
  items: Array<{
    id: string
    title: string
    createdAt: string
    updatedAt: string
    status: 'active' | 'ended'
  }>
  meta?: Record<string, unknown>
}
```

### 3.6 统一错误返回结构（非流式接口）

当接口返回非 2xx 时，建议按以下结构返回：

```ts
type ProtocolErrorBody = {
  ok: false
  error: {
    status: number
    code: string
    message: string
    retryable: boolean
    details?: unknown
  }
  meta: {
    traceId: string
    requestId: string
    protocolVersion: string
    ts: string
    [key: string]: unknown
  }
}
```

说明：
- `retryable=true` 一般表示服务端错误（5xx）可重试。
- 流式接口（`/agent/chat`）在已进入 NDJSON 输出后，错误通常以 `error` 事件形式下发。

### 4) NDJSON 事件约定（建议后端对齐）

建议后端保证这些事件最小字段：

1. `session.start`
   - `payload.sessionId`
   - `payload.messageId`

2. `message.delta`
   - `payload.messageId`
   - `payload.delta`（增量文本）

3. `message.complete`
   - `payload.messageId`

4. `tool.start`
   - `payload.messageId`
   - `payload.toolCallId`
   - `payload.name`

5. `tool.result`
   - `payload.messageId`
   - `payload.toolCallId`
   - `payload.ok`
   - `payload.result` 或 `payload.error`

6. `ui.card / ui.form / ui.chart / ui.action`
   - `payload.messageId`
   - 其余字段按 UI 模块需要提供

7. `status.waiting_user`
   - `payload.enabled = true`

8. `error`
   - `payload.message`
   - `payload.detail`（建议）

9. `session.end`
   - `payload.sessionId`

