# NovaDepot v1.0 测试与验收说明

## 1. 测试体系

NovaDepot v1.0 的测试分为三层：

| 层次 | 工具 | 覆盖范围 | 运行方式 |
|------|------|---------|---------|
| 后端单元测试 | JUnit 5 + Spring Boot Test | Service 层核心逻辑 | `cd backend && mvn test` |
| 前端 E2E 测试 | Playwright | 关键页面流程 | `cd frontend-vue && npm run test:e2e` |
| 数据质量检查 | PowerShell 脚本 | 数据编码、完整性、一致性 | `./scripts/ops/data-quality-check.ps1` |

## 2. E2E 测试 (Playwright)

### 2.1 安装与配置

```bash
cd frontend-vue

# 安装依赖（首次）
npm install

# 安装 Chromium 浏览器（首次）
npx playwright install chromium

# 安装系统依赖（Linux 需要）
npx playwright install-deps chromium
```

### 2.2 运行测试

```bash
# 运行所有 E2E 测试
npm run test:e2e

# 运行指定测试文件
npx playwright test tests/e2e/knowledge.spec.ts

# 带 UI 模式运行
npx playwright test --ui

# 生成和查看报告
npx playwright show-report
```

### 2.3 当前测试用例覆盖

| 测试文件 | 用例数 | 覆盖场景 |
|---------|:------:|---------|
| `login.spec.ts` | 2 | 登录成功、登录失败 |
| `inventory.spec.ts` | 2 | 库存列表加载、库存搜索 |
| `knowledge.spec.ts` | 3 | FAQ 列表、FAQ 新增、SOP 列表 |
| `customer-service.spec.ts` | 3 | 会话列表、消息发送、工单查看 |

**总计：10 个 E2E 测试用例**

### 2.4 测试前准备

1. 确保 Docker 服务全量运行（`docker compose ps` 显示 4 个 up）
2. 确保数据已重置（`./scripts/ops/reset-commercial-baseline.ps1`）
3. 确保数据质量通过（`./scripts/ops/data-quality-check.ps1`）

### 2.5 期望结果

```
Running 10 tests using 1 worker

  ✓ login.spec.ts:2 › 登录成功 - admin 账号 (3.2s)
  ✓ login.spec.ts:2 › 登录失败 - 错误密码 (1.8s)
  ✓ inventory.spec.ts:2 › 库存列表加载 (2.5s)
  ✓ inventory.spec.ts:2 › 库存搜索功能 (2.1s)
  ✓ knowledge.spec.ts:3 › FAQ 列表加载 (2.8s)
  ✓ knowledge.spec.ts:3 › FAQ 新增草稿 (3.5s)
  ✓ knowledge.spec.ts:3 › SOP 列表加载 (2.3s)
  ✓ customer-service.spec.ts:3 › 客服会话列表加载 (3.0s)
  ✓ customer-service.spec.ts:3 › 客服消息发送 (3.8s)
  ✓ customer-service.spec.ts:3 › 工单列表查看 (2.6s)

  10 passed (30s)
```

### 2.6 失败处理

| 失败类型 | 原因 | 处理方式 |
|---------|------|---------|
| `net::ERR_CONNECTION_REFUSED` | 服务未启动 | 启动 Docker |
| `Timeout 30000ms exceeded` | 页面加载慢 | 检查后端日志，增加超时 |
| `expect(...).toBeVisible()` 失败 | 页面元素缺失 | 检查页面路由与组件渲染 |
| `locator.click()` 失败 | 元素未找到 | 检查选择器是否匹配实际 DOM |
| 鉴权失败 401/403 | Token 未获取 | 检查登录步骤 |

## 3. Sprint 3 验收项目

### 3.1 验收页面对照

| 页面 | 路由 | 验收用户 |
|------|------|---------|
| 知识维护入口 | `/cs/workspace` (内嵌) | admin / cs01 |
| 客服工作台 | `/cs/workspace` | cs01 |
| AI 助手 | `/ai/enterprise` | admin / cs01 |
| Agent Center | `/agent/center` | admin / cs01 |
| 审计中心 | `/system/audit-center` | admin |

### 3.2 Sprint 3 验收清单（25 项）

**FAQ 验收 (5 项):**
- [ ] 以 cs01 登录，访问知识维护页面，可查看 FAQ 列表
- [ ] 以 cs01 登录，可新增 FAQ 草稿
- [ ] 以 cs01 登录，可编辑已有 FAQ
- [ ] 以 cs01 登录，可将 FAQ 草稿确认启用
- [ ] 以 cs01 登录，可停用已启用 FAQ

**SOP 验收 (3 项):**
- [ ] 以 cs01 登录，可查看 SOP 列表
- [ ] 以 cs01 登录，可新增 SOP 草稿 → 编辑 → 确认启用
- [ ] 以 cs01 登录，可停用已启用 SOP

**客服沉淀验收 (3 项):**
- [ ] 客服可从工单点击"沉淀为 FAQ"，自动填充草稿
- [ ] 客服可从工单点击"沉淀为 SOP"，自动填充草稿
- [ ] 沉淀的草稿处于"草稿"状态，默认不参与 AI 匹配

**AI 助手验收 (4 项):**
- [ ] AI 助手输入流程类问题（如"怎么入库"），返回含 FAQ/SOP 引用的回复
- [ ] AI 助手输入任务类问题（如"库存补货"），触发 Agent 执行
- [ ] AI 回复中包含"引用来源"标识（FAQ #、SOP #）
- [ ] 历史会话可回溯查看

**客服建议验收 (3 项):**
- [ ] 客服会话中，客户消息到达后 AI 建议框显示候选回复
- [ ] AI 建议回复下方展示 FAQ/SOP/规则依据
- [ ] 客服可点击"采用建议"或手动修改后发送

**Agent 结果验收 (3 项):**
- [ ] Agent 执行结果含摘要卡片（关键数字 + 结论）
- [ ] Agent 结果含表格明细（逐项数据）
- [ ] Agent 结果含"执行依据"（规则名称/数据来源）

**权限控制验收 (2 项):**
- [ ] 以 cs01 登录，访问规则配置页面，修改按钮置灰 + tooltip "仅管理员可操作"
- [ ] 以 cs01 登录，尝试通过 API 修改规则，返回 403

**审计中心验收 (2 项):**
- [ ] 以 admin 登录，可访问审计中心，查看审计日志列表
- [ ] 审计中心可按模块 `knowledge` 筛选，查看 FAQ/SOP 变更记录

### 3.3 通过标准

- **全部 25 项验收通过** → Sprint 3 验收通过
- **1-2 项未通过，非阻塞性**（如 UI 显示微调） → 标记已知问题，不阻塞通过
- **有任何阻塞性未通过**（如 FAQ 无法新增、规则权限校验缺失） → Sprint 3 验收不通过

## 4. 数据质量检查（CI 集成）

```bash
./scripts/ops/reset-commercial-baseline.ps1
./scripts/ops/data-quality-check.ps1
```

通过时输出 `ALL PASS`，否则输出失败项明细。

## 5. 回归测试注意事项

1. 每次数据重置后需重新运行 E2E 测试
2. E2E 测试依赖种子数据中的演示账号和数据
3. 不要修改演示账号的密码、角色
4. 新增功能后需同步更新 E2E 测试用例
5. E2E 测试运行前确保 `frontend-vue` 容器正常运行（代理 API）