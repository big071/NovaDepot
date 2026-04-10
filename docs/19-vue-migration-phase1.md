# NovaDepot 前端迁移执行 - Phase 1（Vue 3 骨架）

## 1. 阶段目标
- 新建并落地独立前端目录：`frontend-vue`。
- 固定技术栈：
  - Vue 3
  - Vite
  - TypeScript
  - Vue Router
  - Pinia
  - Tailwind CSS
  - Naive UI
- ECharts
- Vue 动效方案（原生 Transition + CSS 动效，后续可扩展 Motion）
- 保留旧 Next.js 前端，采用并行迁移模式。
- 接入根 `docker-compose.yml`，支持本地桌面 Docker 启动。

## 2. 本阶段交付范围
- Vue 项目基础脚手架与目录结构。
- 路由与壳层骨架（登录页、后台主框架、仪表盘、占位页面）。
- 统一 API 请求层（Bearer Token + 错误处理）。
- 主题与深浅色模式。
- Dockerfile（前端静态构建 + Nginx 托管）。
- 环境变量示例（`VITE_API_BASE_URL`）。

## 3. 与旧前端并行策略
- 不删除旧 Next.js 工程与运行链路。
- 新前端服务名建议：`frontend-vue`。
- 新前端端口与旧前端分离，避免冲突。

## 4. 目录建议
```text
frontend-vue/
  src/
    layouts/
    pages/
    router/
    stores/
    components/
    services/
    styles/
  public/
  Dockerfile
  nginx.conf
  .env.example
```

## 5. 本阶段验收标准
- `frontend-vue` 可独立构建通过。
- 根 compose 可识别并启动 `frontend-vue`。
- 浏览器可访问 Vue 登录页与仪表盘页。
- API Base URL 可通过 `VITE_API_BASE_URL` 配置。

## 6. 下一阶段衔接（Phase 2）
- 迁移登录页真实流程。
- 迁移后台主框架（侧边栏、顶部栏、内容区）。
- 强化仪表盘视觉层次、图表与动效。
