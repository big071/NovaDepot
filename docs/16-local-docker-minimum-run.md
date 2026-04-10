# NovaDepot 本地 Docker Compose 最小可运行说明

## 1. 目标
- 在不扩展业务功能的前提下，先打通最小可运行链路。
- 使用单条命令启动：`frontend + backend + mysql + redis`。
- 保持当前技术栈：
  - frontend: Next.js + TypeScript
  - backend: Java 17 + Spring Boot 3 + MyBatis-Plus
  - db/cache: MySQL 8 + Redis

## 1.1 前端迁移并行说明（2026-04-10）
- 旧前端（Next.js）保留，作为回退与参考。
- 新前端（Vue 3）使用独立服务 `frontend-vue` 并行运行。
- 建议访问：
  - `http://localhost:3000`（旧 Next.js）
  - `http://localhost:3100`（新 Vue 3）

## 2. 编排与文件约定
- 根目录统一编排：`docker-compose.yml`
- 前端镜像构建：`Dockerfile`
- 后端镜像构建：`backend/Dockerfile`
- MySQL 初始化脚本目录：`backend/deploy/mysql/init`
- 后端 Docker 环境配置：`backend/src/main/resources/application-docker.yml`
- 前端环境变量示例：
  - 本机联调：`.env.example`
  - 容器网络联调：`.env.docker.example`
  - 本地桌面浏览器访问后端时建议：`NEXT_PUBLIC_API_BASE_URL=http://localhost:18080/api/v1`

## 3. 最小运行链路
1. 启动 `mysql` 和 `redis`。
2. 启动 `backend`（`SPRING_PROFILES_ACTIVE=docker`），连接 `mysql/redis`。
3. 启动 `frontend`，通过 `NEXT_PUBLIC_API_BASE_URL=http://backend:8080/api/v1` 访问后端。
4. 访问地址：
   - frontend: `http://localhost:3000`
   - backend swagger: `http://localhost:18080/swagger-ui/index.html`

## 4. 验收标准
- `docker compose config` 校验通过。
- `docker compose up --build` 可成功启动后端、数据库、缓存及前端服务。
- 旧前端与新前端均可访问（并行阶段）。
- 后端 Swagger 可访问。

## 2026-04-10 Phase 2-5 ִ��״̬����

- ����� Vue ǰ��ʣ��׶ε���С����Ǩ�ƣ�
  - Phase 2�������ǲ� + �Ǳ��̣���ʵ API��
  - Phase 3��WMS ����ҳ��
  - Phase 4��AI ���� + ���ܿͷ�����̨
  - Phase 5������ Docker ��֤��·
- ����ѷ��� Vue ������Դ��`http://localhost:3100`��
- ��֤���
  - `docker compose up --build`
  - ���� `http://localhost:3100`��
- ��ϸҳ��/API ���������ձ�׼����`docs/20-vue-migration-phase2-5-execution.md`��

## 2026-04-10 Vue 正式切换补充
- 前端运行入口已切换为 Vue：`http://localhost:3100`。
- Docker Compose 默认服务为：`mysql` + `redis` + `backend` + `frontend-vue`。
- 旧 Next.js 前端已进入清理，不再作为默认运行链路。
- 详细见：`docs/23-vue-cutover-and-next-removal.md`。
