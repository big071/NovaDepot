# NovaDepot v1.0 部署与运维说明

## 1. 前置要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| Docker | 20.10+ | 容器运行时 |
| Docker Compose | 2.x | 容器编排 |
| Git | 任意 | 拉取代码 |
| Windows / macOS / Linux | — | 均支持 |

无需本地安装 Java、Node.js、MySQL、Redis，全部由 Docker 提供。

## 2. 快速启动（首次部署）

### 2.1 拉取代码

```bash
git clone <repo-url> NovaDepot
cd NovaDepot
```

### 2.2 复制环境变量

```bash
cp backend/.env.example backend/.env
cp frontend-vue/.env.example frontend-vue/.env
```

### 2.3 启动所有服务

```bash
docker compose up -d
```

### 2.4 初始化数据库

```bash
./scripts/ops/reset-commercial-baseline.ps1    # Windows
# 或
./scripts/ops/reset-commercial-baseline.sh     # Linux/macOS
```

### 2.5 验证

```bash
docker compose ps
```
预期输出：四个服务（mysql, redis, backend, frontend-vue）均为 `Up` 状态。

### 2.6 访问

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:3100 |
| Swagger API 文档 | http://localhost:18080/swagger-ui/index.html |

## 3. 服务端口

| 服务 | 容器内端口 | 宿主机端口 | 说明 |
|------|----------|----------|------|
| MySQL | 3306 | 3306 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |
| Backend (Spring Boot) | 18080 | 18080 | REST API |
| Frontend (Nginx) | 80 | 3100 | Vue 静态文件 |

## 4. 常用运维命令

### 4.1 服务管理

```bash
# 启动所有服务
docker compose up -d

# 停止所有服务
docker compose down

# 重启单个服务
docker compose restart backend
docker compose restart frontend-vue

# 查看日志
docker compose logs -f backend
docker compose logs -f frontend-vue
docker compose logs -f backend --tail=100   # 最近 100 行

# 查看状态
docker compose ps
```

### 4.2 数据管理

```bash
# 重置数据（恢复到基线种子数据）
./scripts/ops/reset-commercial-baseline.ps1

# 数据质量检查
./scripts/ops/data-quality-check.ps1

# 备份数据库
docker exec novadepot-mysql mysqldump -u root -pnovadepot123 novadepot > backup-$(date +%Y%m%d).sql
```

### 4.3 进入容器排查

```bash
# 进入后端容器
docker exec -it novadepot-backend bash

# 进入 MySQL
docker exec -it novadepot-mysql mysql -u root -pnovadepot123 novadepot

# 进入 Redis
docker exec -it novadepot-redis redis-cli
```

## 5. 数据重置（reset-commercial-baseline）

### 作用

将数据库恢复到商业基线状态，包含：
- 演示用户（admin, warehouse01, cs01, observer01）
- 角色与权限
- 种子产品、仓库、库位
- 种子 FAQ、SOP、规则配置
- 示例入库/出库单
- 示例库存数据
- 示例客服会话/工单

### 执行

```bash
./scripts/ops/reset-commercial-baseline.ps1
```

### 注意事项

- **会清空全部现有数据**，仅保留种子数据
- 开发/测试环境专用，不可在生产环境执行
- 执行后建议运行 `data-quality-check` 验证数据完整性

## 6. 数据质量检查（data-quality-check）

### 作用

自动检查数据库中的关键字段，确保：
- 中文字段无乱码
- 外键关联完整性
- 关键表数据量合理
- 权限角色数据完整
- 库存数据与事务一致性

### 执行

```bash
./scripts/ops/data-quality-check.ps1
```

### 预期输出

通过时显示 `PASS`，不通过时显示 `FAIL` 及具体失败项。

## 7. 镜像构建

### 7.1 构建后端镜像

```bash
cd backend
docker build -t novadepot-backend:latest .
```

或使用 Maven 打包后构建：

```bash
cd backend
mvn clean package -DskipTests
docker build -t novadepot-backend:latest .
```

### 7.2 构建前端镜像

```bash
cd frontend-vue
docker build -t novadepot-frontend-vue:latest .
```

### 7.3 一键构建

```bash
docker compose build
```

## 8. 环境变量说明

### 8.1 Backend (.env)

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_HOST` | mysql | MySQL 容器名 |
| `MYSQL_PORT` | 3306 | MySQL 端口 |
| `MYSQL_DATABASE` | novadepot | 数据库名 |
| `MYSQL_USER` | root | 数据库用户 |
| `MYSQL_PASSWORD` | novadepot123 | 数据库密码 |
| `REDIS_HOST` | redis | Redis 容器名 |
| `REDIS_PORT` | 6379 | Redis 端口 |
| `JWT_SECRET` | novadepot-secret-key-2024 | JWT 签名密钥 |
| `AI_PROVIDER` | rule | AI Provider 类型 |
| `AI_DEEPSEEK_API_KEY` | — | DeepSeek API Key（付费时使用） |

### 8.2 Frontend (.env)

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `VITE_API_BASE_URL` | http://localhost:18080 | 后端 API 地址 |

## 9. 故障排查

### 9.1 容器无法启动

```bash
# 查看具体错误日志
docker compose logs backend
docker compose logs mysql

# 检查端口占用
netstat -ano | findstr 3306   # Windows
lsof -i :3306                # macOS/Linux
```

### 9.2 数据库连接失败

检查后端日志中 MySQL 连接参数：
```bash
docker compose logs backend | grep -i "mysql\|datasource\|connection"
```

常见原因：
- MySQL 容器未就绪（等待 `healthy` 后再启动 backend）
- `.env` 中密码不匹配
- MySQL 容器名与配置不一致

### 9.3 API 返回 500 错误

```bash
docker compose logs backend --tail=50
```
确认是否有：
- 数据库表未初始化 → 执行 `reset-commercial-baseline`
- SQL 拼写错误 → 检查后端日志中的异常栈

### 9.4 Vue 页面白屏

```bash
docker compose logs frontend-vue --tail=20
```
检查：
- Nginx 是否正确代理 API 请求到 `http://backend:18080`
- 浏览器控制台是否有 CORS 或 404 错误

## 10. 生产部署注意事项

> ⚠️ **当前版本定位为本地 Docker Compose 部署**，生产环境部署需额外配置：

1. **数据库持久化**：确保 MySQL 数据目录挂载到宿主机
2. **密钥管理**：替换所有默认密码和 secret
3. **HTTPS**：在 Nginx 或前置负载均衡配置 TLS
4. **监控与日志**：接入日志收集与监控系统
5. **资源限制**：设置容器 CPU/内存限制
6. **备份策略**：配置定期数据库备份
7. **网络策略**：仅暴露必要端口，数据库不对外