# NovaDepot AI 对话最小可用链路说明

## 1. 目标
- 仅打通 AI 对话发送链路，不扩展业务功能。
- 使用免费 Provider：`mock` / `rule`。
- 保持现有技术栈与模块边界。

## 2. 前后端接口约定
- 登录接口：`POST /api/v1/auth/login`
- AI 对话接口：`POST /api/v1/ai/chat`
- AI 会话列表接口：`GET /api/v1/ai/conversations`
- 统一响应结构：`{ code, message, data, traceId, timestamp }`

请求示例：

```json
{
  "conversationId": 123,
  "scene": "enterprise",
  "message": "请给我库存概览",
  "providerHint": "rule"
}
```

## 3. 本地 Docker 联调配置
- 前端访问地址：`http://localhost:3000`
- 后端 Swagger：`http://localhost:18080/swagger-ui/index.html`
- 前端 `NEXT_PUBLIC_API_BASE_URL`（浏览器侧）应指向：
  - `http://localhost:18080/api/v1`

说明：
- 不要在浏览器侧使用 `http://backend:8080`，该地址仅容器内部可见。

## 4. 免费 Provider 约束
- 默认 `AI_PROVIDER=rule`
- 必须 `AI_PAID_ENABLED=false`
- 当 `rule` 异常时，`AiService` 允许回退到 `mock` 响应

## 5. 验收标准
1. 在 AI 页面输入消息并发送，前端发出 `POST /api/v1/ai/chat`。
2. 后端返回 `rule/mock` 回复，不依赖收费 API。
3. 页面显示用户消息与 AI 回复。
4. 请求失败时前端可见明确错误提示。

