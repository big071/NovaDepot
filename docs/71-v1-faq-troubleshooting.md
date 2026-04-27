# NovaDepot v1.0 FAQ 排错手册

## 1. 部署与启动问题

### Q1: `docker compose up -d` 报错

**A:** 检查步骤：

```bash
# 确认 Docker 版本
docker --version
docker compose version

# 确认端口未被占用
netstat -ano | findstr :3306
netstat -ano | findstr :6379
netstat -ano | findstr :18080
netstat -ano | findstr :3100

# 清理旧容器和网络后重试
docker compose down -v
docker compose up -d
```

### Q2: MySQL 容器无法启动

**A:** 常见原因：

1. 端口 3306 被本地 MySQL 占用 → 停止本地 MySQL 或修改映射端口
2. 数据卷权限问题 → `docker compose down -v && docker volume prune -f`
3. 检查磁盘空间：`docker system df`

### Q3: Backend 容器反复重启

**A:** 查看日志定位：

```bash
docker compose logs backend --tail=100
```

常见错误：
- `Table 'novadepot.xxx' doesn't exist` → 未执行 `reset-commercial-baseline`
- `Connection refused to mysql:3306` → MySQL 未就绪，重启 backend
- `Access denied for user 'root'@'...'` → 密码不匹配，检查 `.env`

### Q4: Frontend-Vue 无法访问后端 API

**A:** 检查：

1. 确认 `frontend-vue/nginx.conf` 中 `proxy_pass` 指向 `http://backend:18080`
2. 浏览器访问 `http://localhost:18080/swagger-ui/index.html` 确认后端正常
3. 浏览器 F12 → Network → 查看 XHR 请求是否返回 502/504
4. 如果是 CORS 错误，检查后端 `CorsConfig` 是否允许 `localhost:3100`

---

## 2. 登录与权限问题

### Q5: 登录提示"用户名或密码错误"

**A:** 

1. 确认是否执行了 `reset-commercial-baseline.ps1`
2. 确认使用正确的演示账号：
   - `admin / admin123`
   - `warehouse01 / pass123`
   - `cs01 / pass123`
   - `observer01 / pass123`
3. 如果修改过密码，需要重新 reset baseline

### Q6: 登录后跳转到 403 页面

**A:** 检查用户角色是否有该页面的访问权限：

- 非 admin 访问 `/system/audit-center` → 403（正常行为）
- warehouse 访问客服工作台 → 403（正常行为）
- 确认前端路由守卫 `requiredPermissions` 配置是否与用户角色匹配

参照 `docs/63-v1-roles-and-permissions.md` 中的权限矩阵。

### Q7: JWT Token 过期

**A:** Token 有效期默认 24 小时。过期后需重新登录。

检查后端日志 `docker compose logs backend | grep -i "token\|jwt"`。

---

## 3. FAQ / SOP 知识库问题

### Q8: FAQ 列表为空

**A:** 

1. 检查是否执行了 `reset-commercial-baseline.ps1`
2. 运行 `data-quality-check.ps1` 确认 FAQ 表数据量
3. 检查 MySQL：`docker exec -it novadepot-mysql mysql -u root -pnovadepot123 -e "SELECT COUNT(*) FROM novadepot.faq_knowledge;"`
4. 如果为 0，重新执行 baseline 脚本

### Q9: FAQ 草稿无法"确认启用"

**A:** 

1. 确认草稿的必填字段（question, answer）均已填写
2. 确认当前用户有 `faq:manage` 权限（admin 或 cs 角色）
3. 检查后端日志是否有异常

### Q10: AI 助手回复不含 FAQ/SOP 引用

**A:** 

1. 确认 FAQ/SOP 中有已启用的条目（草稿不会被引用）
2. 检查 `AiProvider` 配置是否为 `rule`（不是 mock）
3. 输入的问题是否与 FAQ 的关键词匹配
4. 运行 `data-quality-check.ps1` 确认知识库数据正常

---

## 4. 客服工作台问题

### Q11: 客服会话列表为空

**A:** 

1. 确认执行了 `reset-commercial-baseline.ps1`
2. 运行 `data-quality-check.ps1` 确认客服数据正常
3. 如果是全新环境未初始化，需要执行 baseline 脚本

### Q12: AI 建议回复框不显示候选内容

**A:** 

1. 确认知识库中有已启用的 FAQ
2. 确认客户消息包含可匹配的关键词
3. 检查后端 `RuleAiProvider` 日志
4. 确认 AI 回复模式是否为"建议模式"

### Q13: "沉淀为 FAQ"按钮灰显/不响应

**A:** 

1. 确认当前用户为客服角色（cs01）或管理员
2. 确认工单状态不是"草稿"（只有处理中/已关闭的工单可沉淀）
3. 检查用户是否有 `faq:manage` 权限

---

## 5. Agent 与 AI 助手问题

### Q14: Agent 执行超时

**A:** 

1. Agent 默认超时 30 秒
2. 如果库存数据量大，可能需要更长时间
3. 检查后端日志 `docker compose logs backend | grep -i agent`

### Q15: AI 助手无法识别意图

**A:** 

1. 当前为规则级意图识别，非深度学习
2. 尝试使用更明确的关键词：如"补货"而非"仓库里什么东西不够了"
3. 检查 `AiProvider` 配置是否为 `rule`

---

## 6. 数据库与编码问题

### Q16: 中文显示为乱码 "???" 或 ""

**A:** 按 AGENTS.md 中的编码规则排查：

```bash
# 检查数据库编码
docker exec -it novadepot-mysql mysql -u root -pnovadepot123 -e "SHOW CREATE DATABASE novadepot;"

# 检查表编码
docker exec -it novadepot-mysql mysql -u root -pnovadepot123 -e "SELECT TABLE_NAME, TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA='novadepot' LIMIT 5;"

# 检查连接参数
docker compose logs backend | grep -i "charset\|encoding\|utf8\|character"
```

修复流程：
1. 确认数据库 charset = `utf8mb4`
2. 确认 JDBC URL 含 `useUnicode=true&characterEncoding=utf-8`
3. 确认 SQL 文件编码为 UTF-8 without BOM
4. 重新执行 `reset-commercial-baseline`

### Q17: `reset-commercial-baseline.ps1` 执行失败

**A:** 

1. 确认 MySQL 容器正在运行：`docker ps | findstr mysql`
2. 确认 `backups/novadepot-backup-20260420-140401.sql` 文件存在
3. 确认 PowerShell 执行策略：`Get-ExecutionPolicy` → 如果是 Restricted，运行 `Set-ExecutionPolicy RemoteSigned -Scope CurrentUser`
4. 手动测试 MySQL 连接：`docker exec -it novadepot-mysql mysql -u root -pnovadepot123 -e "SELECT 1;"`

---

## 7. E2E 测试问题

### Q18: Playwright 测试全部失败 (net::ERR_CONNECTION_REFUSED)

**A:** 

1. 确认 Docker 服务全量运行：`docker compose ps`
2. 确认前端端口 3100 可访问：浏览器打开 `http://localhost:3100`
3. 确认后端端口 18080 可访问：浏览器打开 `http://localhost:18080/swagger-ui/index.html`

### Q19: Playwright 报 `chromium not found`

**A:** 

```bash
cd frontend-vue
npx playwright install chromium
```

### Q20: 某个测试用例超时

**A:** 

1. 增加超时时间：修改 `playwright.config.ts` 中的 `timeout: 30000` -> `timeout: 60000`
2. 检查后端是否有慢查询：`docker compose logs backend | grep -i "slow\|timeout"`
3. 单独运行该用例排查：`npx playwright test tests/e2e/xxx.spec.ts --debug`

---

## 8. 数据质量问题

### Q21: `data-quality-check.ps1` 报告 FAIL

**A:** 按失败项逐一排查：

| 失败项 | 排查方向 |
|--------|---------|
| Table xxx: 0 rows | 确认执行了 baseline 脚本 |
| Chinese encoding FAIL | 按 Q16 排查编码问题 |
| Foreign key FAIL | 检查关联表数据完整性 |
| Inventory consistency FAIL | 检查库存事务记录与库存表对比 |

---

## 9. 仍无法解决？

1. 先查阅对应模块的 docs/ 文档
2. 检查 `docker compose logs` 获取详细错误信息
3. 运行 `docker compose down -v && docker compose up -d && ./scripts/ops/reset-commercial-baseline.ps1` 从头重置
4. 确认使用 Docker Compose 2.x（`docker compose ps` vs `docker-compose ps`）