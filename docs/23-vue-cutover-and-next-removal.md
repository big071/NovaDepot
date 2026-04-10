# NovaDepot Vue 正式切换与旧 Next.js 清理记录

## 1. 目标
在 Vue 前端验收通过后，执行旧 Next.js 前端清理，保留后端与基础设施不变：
- Java 17+ / Spring Boot 3
- MySQL 8 / Redis
- MyBatis-Plus
- Docker Compose 本地运行

## 2. 本次变更范围
1. 切换 Docker Compose 到 Vue 单前端服务（`frontend-vue`）。
2. 删除旧 Next.js 前端目录与配置。
3. 清理构建垃圾文件（如 `.next`、`node_modules`、`dist`）。
4. 更新 README 与运行说明。

## 3. 保留内容
- `backend/`
- `backend/deploy/mysql/init/`
- `frontend-vue/`
- `docker-compose.yml`（保留 mysql/redis/backend/frontend-vue）
- `docs/`

## 4. 删除内容（本轮执行）
- 旧 Next.js 目录：`app/` `components/` `lib/` `public/` `store/` `.next/`
- 旧 Next.js 配置：`Dockerfile` `next.config.mjs` `next-env.d.ts` `postcss.config.mjs` `tailwind.config.ts` `tsconfig.json`
- 旧 Next.js 依赖：根 `package.json` `package-lock.json` 根 `node_modules/`
- 旧 compose 文件：`docker-compose.frontend.yml`
- 根目录 Next 环境示例：`.env.example` `.env.docker.example`

## 5. 验收要求
1. `docker compose up --build -d` 成功。
2. `http://localhost:3100` 可访问。
3. 后端接口联调（登录/仪表盘/商品/库存/AI/客服）可用。

## 6. 回退方式
- 若需回退旧 Next.js，建议从 Git 历史恢复相关目录与配置，不在当前目录保留双前端并行。
