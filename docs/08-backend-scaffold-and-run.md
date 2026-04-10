# NovaDepot 后端脚手架落地与运行说明（第一版）

## 1. 本轮目标
本轮生成 Java 17 + Spring Boot 3 后端可运行骨架，覆盖：
- 基础工程能力（配置、异常、响应、日志、参数校验）
- MyBatis-Plus、MySQL 8、Redis 接入
- Spring Security + JWT + RBAC 权限骨架
- OpenAPI/Swagger 配置
- 模块骨架（auth/users/roles/permissions/products/warehouses/inventory/inbound-orders/outbound-orders/reports/notifications/audit-logs/ai/customer-service）
- 多租户与 AI Provider 抽象预留

## 2. 目录与命名约定
- 后端代码放在 `backend/` 子目录，不影响前端工程。
- 包名：`com.novadepot.backend`
- API 前缀：`/api/v1`
- 配置文件：
  - `application.yml`
  - `application-dev.yml`
  - `application-docker.yml`

## 3. 本地运行
1. 先准备 MySQL 8 与 Redis（本机或 Docker）。
2. 本机需安装 JDK 17+ 与 Maven 3.9+。
2. 在 `backend/` 执行：
   - `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
3. Swagger 地址：
   - `http://localhost:8080/swagger-ui/index.html`

若本机无 Maven，可直接使用 Docker：
- `docker compose -f backend/docker-compose.backend.yml up --build`

## 4. Docker 运行
- 提供 `backend/Dockerfile`
- 可通过 `docker-compose.backend.yml` 启动：
  - backend
  - mysql
  - redis
- 容器网络内连接：
  - MySQL 主机名：`mysql`
  - Redis 主机名：`redis`

## 5. 环境变量建议
- `SPRING_PROFILES_ACTIVE=docker`
- `DB_HOST=mysql`
- `DB_PORT=3306`
- `DB_NAME=novadepot`
- `DB_USERNAME=root`
- `DB_PASSWORD=root`
- `REDIS_HOST=redis`
- `REDIS_PORT=6379`
- `JWT_SECRET=change_me_in_prod`

## 6. 后续衔接
- 第二步补齐 DDL/init.sql 与迁移脚本（Flyway/Liquibase）。
- 第三步将骨架模块逐步替换为真实业务实现（库存流水、单据状态流转、客服工单、AI 编排）。

## 7. 本轮库存闭环已落地接口（MVP）
- `POST /api/v1/products`
- `POST /api/v1/warehouses`
- `POST /api/v1/locations`
- `GET /api/v1/inventory`
- `GET /api/v1/inventory/transactions`
- `GET /api/v1/inventory/alerts/low-stock`
- `POST /api/v1/inbound-orders`
- `POST /api/v1/inbound-orders/{id}/actions/approve`
- `POST /api/v1/inbound-orders/{id}/actions/post`
- `POST /api/v1/outbound-orders`
- `POST /api/v1/outbound-orders/{id}/actions/approve`
- `POST /api/v1/outbound-orders/{id}/actions/ship`
- `GET /api/v1/reports/dashboard`

## 8. AI Provider 本地与 Docker 配置补充（免费阶段）
- 默认推荐：`AI_PROVIDER=rule`，`AI_PAID_ENABLED=false`
- 可选切换：`AI_PROVIDER=mock`（联调/演示）
- 规则阈值：
  - `AI_RULE_LOW_STOCK_THRESHOLD`（低库存阈值）
  - `AI_RULE_ABNORMAL_CHANGE_THRESHOLD`（异常变动阈值）
- 关键接口：
  - `POST /api/v1/ai/chat`
  - `GET /api/v1/ai/conversations`
- 数据落库：`ai_conversations`、`ai_messages`、`ai_prompt_templates`
- 前端联调：`NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1`

## 9. 智能客服模块 Docker 联调补充

### 9.1 前端环境变量
- `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1`

### 9.2 客服工作台联调路径
- 页面：`/cs/workspace`
- 会话列表：`GET /api/v1/customer-service/sessions`
- 会话消息：`GET /api/v1/customer-service/sessions/{id}/messages`
- 消息发送：`POST /api/v1/customer-service/sessions/{id}/messages`
- 人工转接：`POST /api/v1/customer-service/sessions/{id}/actions/transfer-human`

### 9.3 后端运行依赖
- MySQL：客服会话、消息、规则、FAQ 基础表由 `backend/deploy/mysql/init` 自动初始化。
- Redis：用于会话未读计数、坐席在线状态与限流计数（后续增强）。

### 9.4 本地桌面运行建议
1. 启动后端编排：`docker compose -f backend/docker-compose.backend.yml up -d`
2. 启动前端：按前端文档运行（确保 API Base 指向后端）。
3. 打开：`http://localhost:3000/cs/workspace` 进行客服工作台联调。

## 10. 多租户与商业化运行配置补充

### 10.1 当前默认模式
- 本地与开发环境默认单租户：`APP_TENANT_MODE=single`
- 默认租户：`tenant_id=1`

### 10.2 预留环境变量（商用阶段）
- `APP_TENANT_MODE=single|multi`
- `APP_BILLING_ENABLED=false|true`
- `APP_PLAN_ENFORCE_ENABLED=false|true`
- `AI_PAID_ENABLED=false|true`

### 10.3 Docker Compose 建议
- 本地桌面联调继续使用单套编排（backend + mysql + redis + frontend）。
- 通过环境变量切换运行模式，避免维护多套镜像。
- 租户级运行时配置优先落 DB（`tenant_settings` 规划），Redis 做缓存。


## 2026-04-10 ���ض˿ڲ���
- ��ǰ��Ŀ¼ docker-compose.yml ���Ⱪ¶��˶˿�Ϊ 18080��
- Swagger ���ط��ʵ�ַ��http://localhost:18080/swagger-ui/index.html
