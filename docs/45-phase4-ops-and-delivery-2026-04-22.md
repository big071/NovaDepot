# NovaDepot 商用版本 Phase 4：运维与交付能力（2026-04-22）

## 1. 阶段目标
补齐商用基础版所需的运维、交付、备份恢复、发布回滚能力，使项目具备可执行、可验证、可复盘的交付属性。

## 2. 适用范围与边界
1. 适用环境：Docker 本地环境（`mysql + redis + backend + frontend-vue`）。
2. 当前架构：单租户（`tenant_code=default`）。
3. 非目标：
   1. 不扩展业务功能模块；
   2. 不引入新中间件；
   3. 不变更核心技术栈。

## 3. 部署说明（Docker）
1. 先决条件：
   1. 已安装 Docker Desktop；
   2. Docker Daemon 处于运行状态；
   3. 当前目录为仓库根目录 `D:\新建文件夹\NovaDepot`。
2. 启动：
   1. `docker compose up -d mysql redis backend frontend-vue`
3. 检查：
   1. `docker compose ps`
   2. 后端健康检查：`http://localhost:18080/actuator/health`
   3. 前端访问：`http://localhost:3100`

## 4. 初始化说明
1. 首次初始化（自动）：
   1. `docker compose up` 时 MySQL 会执行 `backend/deploy/mysql/init/*.sql`。
2. 手动重灌（推荐用于演示前）：
   1. RC 演示基线：`./scripts/ops/reset-rc-baseline.ps1`
   2. 商用样本基线：`./scripts/ops/reset-commercial-baseline.ps1`
3. 注意：
   1. 重置会覆盖现有演示数据；
   2. 建议先执行备份脚本。

## 5. 数据重置说明
1. RC 演示基线：
   1. 脚本：`scripts/ops/reset-rc-baseline.ps1`
   2. 执行顺序：`97-reset-demo-data.sql -> 99-seed-mvp.sql`
2. 商用样本基线：
   1. 脚本：`scripts/ops/reset-commercial-baseline.ps1`
   2. 执行顺序：`96-reset-business-sample.sql -> 99-seed-mvp.sql -> 100-seed-business-sample.sql -> 101-seed-stress-lite.sql`
3. 风险提示：
   1. 会清空目标区间样本数据；
   2. 运行中业务操作会被覆盖；
   3. 需确保 MySQL 容器可连接。

## 6. 备份与恢复说明
1. 备份：
   1. `./scripts/ops/backup.ps1`
   2. 自定义目录：`./scripts/ops/backup.ps1 -OutputDir ./backups`
2. 恢复：
   1. `./scripts/ops/restore.ps1 -BackupFile ./backups/novadepot-backup-YYYYMMDD-HHMMSS.sql`
3. 风险与注意事项：
   1. 恢复会覆盖当前库数据；
   2. 恢复期间避免并发写操作；
   3. 建议恢复前再次备份当前状态。

## 7. 测试运行说明
1. 本地脚本化检查：
   1. `./scripts/ops/release-checklist.ps1`
   2. `./scripts/ops/data-quality-check.ps1`
2. 检查内容（最小闭环）：
   1. 容器状态；
   2. 后端健康；
   3. 登录可用；
   4. 仪表盘接口；
   5. 低库存告警接口；
   6. 工单分页接口；
   7. AI 对话接口。
3. 数据可信度补充检查（Sprint 50）：
   1. 乱码字符抽检（`????`、`�`、`ï»¿`）；
   2. 关键字段空值抽检（商品、仓库、库位、库存、工单、FAQ、AI）；
   3. 仪表盘指标与明细数据一致性；
   4. 低库存预警与安全库存口径一致性。

## 8. 发布与回滚标准

### 8.1 发布前检查清单
1. Docker Daemon 可用。
2. `docker compose ps` 四个核心服务状态正常。
3. 已执行并通过 `release-checklist.ps1`。
4. 已执行备份并确认备份文件可读。
5. 本次发布变更范围、回滚点、责任人明确。

### 8.2 发布步骤说明（本地/预发）
1. 拉取代码并确认变更。
2. 可选备份：`./scripts/ops/backup.ps1`
3. 构建并重启：
   1. `docker compose up -d --build backend frontend-vue`
4. 执行发布后验证：
   1. `./scripts/ops/release-checklist.ps1`
5. 执行场景化验收（见文档 46）。

### 8.3 回滚步骤说明
1. 快速回滚（数据不变）：
   1. 回滚代码版本；
   2. `docker compose up -d --build backend frontend-vue`。
2. 数据回滚（必要时）：
   1. `./scripts/ops/restore.ps1 -BackupFile <备份文件>`
3. 校验：
   1. `./scripts/ops/rollback-checklist.ps1 -BackupFile <备份文件>`

### 8.4 发布后验证清单
1. 登录成功，核心角色可访问。
2. 仪表盘指标正常返回。
3. 入库/出库/客服/AI 基础链路可运行。
4. 审计中心可查询新增日志。
5. 前端关键页面无明显错误提示。

### 8.5 故障处理建议
1. 优先确认是否为环境问题（Docker/网络/端口）。
2. 再确认数据问题（种子脚本执行、字段缺失、旧库结构）。
3. 最后定位代码问题（后端日志 + 接口返回 + 前端控制台）。

## 9. 日志与异常处理说明

### 9.1 关键错误日志定位方式
1. 后端容器日志：`docker logs --tail 300 novadepot-backend`
2. MySQL 日志：`docker logs --tail 200 novadepot-mysql`
3. 前端容器日志：`docker logs --tail 200 novadepot-frontend-vue`
4. 全局状态：`docker compose ps`

### 9.2 关键模块异常排查
1. 登录失败/鉴权失败：
   1. 检查 `users` 表安全字段是否齐全；
   2. 检查 token 与权限点；
   3. 检查 `ForcePasswordChangeFilter` 拦截行为。
2. 入出库状态流转异常：
   1. 检查单据当前状态；
   2. 检查角色权限；
   3. 检查库存与流水是否存在冲突。
3. 客服工单异常：
   1. 检查 session 是否存在；
   2. 检查工单状态/责任人字段；
   3. 检查审计是否写入。
4. AI 建议异常：
   1. 检查 AI provider 配置；
   2. 检查样本数据（低库存SKU）；
   3. 检查会话与消息落库。

### 9.3 审计 / 业务日志 / 系统日志使用方式
1. 审计日志：用于回放谁在何时做了什么（写操作追踪）。
2. 业务日志：用于定位业务流程中断点（单据、库存、客服、AI）。
3. 系统日志：用于定位服务不可用、连接失败、启动异常。

### 9.4 Docker 常见故障诊断路径
1. `dockerDesktopLinuxEngine` 不存在：
   1. 启动 Docker Desktop；
   2. 重试 `docker compose ps`。
2. MySQL 连接失败：
   1. 检查 `novadepot-mysql` 健康状态；
   2. 检查 `13306` 端口占用。
3. 后端 500：
   1. 查看后端日志栈；
   2. 检查数据库 schema 与 seed 是否匹配。

## 10. 演示说明（交付口径）
1. 演示前执行：
   1. `./scripts/ops/reset-commercial-baseline.ps1`
   2. `./scripts/ops/release-checklist.ps1`
2. 演示路径建议：
   1. 先仪表盘总览；
   2. 入库/出库链路；
   3. 客服建单与回查；
   4. AI 补货建议与风险解释。
3. 录屏与截图节点基线见：
   1. `docs/46-phase4-scenario-screenshot-baseline-2026-04-22.md`

## 11. 历史乱码段治理（99-seed-mvp.sql）
1. 根因：
   1. 历史 MVP 种子在不同终端编码链路下被覆盖，部分中文文本退化为 `????`。
   2. 该旧段虽然可被后置脚本部分覆盖，但覆盖不完整，存在误用风险。
2. 本次修复：
   1. 在 `99-seed-mvp.sql` 末尾补充“后置文本归一化”覆盖范围，新增对租户名、分类名、单位名、仓库地址、入库备注、出库备注的统一修正。
   2. 保留幂等更新策略（`UPDATE ... WHERE tenant_id=1 / id=1`），确保 RC/商用重置多次执行结果一致。
3. 验证方法：
   1. 执行 `./scripts/ops/reset-commercial-baseline.ps1`。
   2. 在 MySQL 中检查关键字段不存在 `????`、`�`、`ï»¿`（租户、商品分类、单位、仓库、入库单、出库单、FAQ、客服消息、AI 消息）。
   3. 运行 `./scripts/ops/release-checklist.ps1` 并确认通过。
4. 预防规则：
   1. 所有 SQL/文档统一 UTF-8（无 BOM），数据库全链路保持 `utf8mb4`。
   2. 禁止直接复用未归一化的历史段做演示基线。
   3. 后续若新增中文样本，必须补充“重置后抽检 SQL”并纳入交付清单。
