# NovaDepot

NovaDepot 当前已切换为 Vue 3 前端 + Spring Boot 后端的本地 Docker 运行模式。

## 技术栈
- Frontend: Vue 3 + Vite + TypeScript + Vue Router + Pinia + Tailwind CSS + Naive UI + ECharts
- Backend: Java 17 + Spring Boot 3 + MyBatis-Plus
- Database: MySQL 8
- Cache: Redis

## 本地启动
在仓库根目录执行：

```bash
docker compose up --build
```

## 访问地址
- Vue 前端: `http://localhost:3100`
- 后端 API: `http://localhost:18080`
- Swagger: `http://localhost:18080/swagger-ui/index.html`

## 关键目录
- `frontend-vue/` Vue 前端
- `backend/` Spring Boot 后端
- `backend/deploy/mysql/init/` MySQL 初始化脚本
- `docs/` 项目文档

## 环境变量
- Vue 示例: `frontend-vue/.env.example`
- Docker Compose 中默认使用:
  - `VITE_API_BASE_URL=http://localhost:18080/api/v1`
