# NovaDepot v1.0 数据初始化与重置说明

## 1. 数据初始化流程

```
启动 MySQL 容器
  → docker compose up -d mysql
  → 等待 MySQL healthy

执行 reset-commercial-baseline
  → 连接 MySQL
  → 删除旧库，重建 novadepot
  → 执行 schema DDL（建表）
  → 执行商业基线种子数据 INSERT
  → 验证数据完整性
```

## 2. 种子数据内容

### 2.1 用户与权限

```
用户表 (users):
  admin        / admin123    → 角色: admin
  warehouse01  / pass123     → 角色: warehouse
  cs01         / pass123     → 角色: cs
  observer01   / pass123     → 角色: observer

角色表 (roles): admin, warehouse, cs, observer
权限表 (permissions): 见 docs/63-v1-roles-and-permissions.md
角色-权限关联 (role_permissions): 按上文权限矩阵关联
用户-角色关联 (user_roles): 按上文用户角色分配
```

### 2.2 主数据

```
仓库 (warehouses):
  WH-MAIN  → 上海主仓
  WH-SUB   → 昆山分仓

库位 (locations):
  WH-MAIN-A-01 ~ WH-MAIN-A-20  (主仓 A 区)
  WH-MAIN-B-01 ~ WH-MAIN-B-15  (主仓 B 区)
  WH-SUB-A-01 ~ WH-SUB-A-10    (分仓 A 区)

产品 (products):
  PROD-001 螺丝 M8         标准件
  PROD-002 螺母 M8         标准件
  PROD-003 垫圈 M10        标准件
  PROD-004 弹簧 M6         弹性件
  PROD-005 密封圈 50mm     密封件
  PROD-006 轴承 6205       传动件
  PROD-007 润滑油 5L       耗材
  PROD-008 清洁剂 10L      耗材
  PROD-009 包装膜 50m      包材
  PROD-010 标签纸 (1000p/卷) 包材
```

### 2.3 知识库

```
FAQ (faq_knowledge):
  10 条常见问题（已启用 8 条，草稿 2 条），覆盖：
  - 入库流程咨询
  - 出库问题处理
  - 退货处理
  - 库存查询方法
  - 系统登录问题
  - 条码异常处理
  - 产品损坏处理
  - 客服联系

SOP (sop_knowledge):
  5 条标准操作流程（已启用 3 条，草稿 2 条），覆盖：
  - 标准入库流程
  - 标准出库流程
  - 退货处理流程
  - 库位移库流程（草稿）
  - 盘点流程（草稿）

规则配置 (rule_config):
  low_stock_threshold = 50
  critical_stock_threshold = 10
  auto_reply_priority_threshold = 3
  ticket_auto_close_days = 7
  knowledge_draft_requires_review = true
```

### 2.4 业务数据

```
入库单 (inbound_orders):
  8 条（已完成 5，待执行 1，待审核 1，草稿 1）

出库单 (outbound_orders):
  6 条（已完成 3，待发运 1，待审核 1，草稿 1）

库存 (inventory):
  按产品+库位分布，包含低库存场景（螺丝 M8 = 15，螺母 M8 = 30）

库存事务 (inventory_transactions):
  按入库/出库操作生成事务记录

客服会话 (customer_service_sessions):
  3 个演示会话

客服消息 (customer_service_messages):
  每个会话 3-6 条消息

客服工单 (customer_service_tickets):
  2 条工单（已关闭 1，处理中 1）
```

## 3. reset-commercial-baseline.ps1 说明

### 位置

`scripts/ops/reset-commercial-baseline.ps1`

### 前置条件

- MySQL 容器运行中
- 宿主机安装了 `mysql` 客户端（用于连接 MySQL）
- PowerShell 执行策略允许脚本运行

### 工作原理

1. 通过 `docker exec` 连接 MySQL
2. `DROP DATABASE IF EXISTS novadepot; CREATE DATABASE novadepot CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
3. 执行 `backups/novadepot-backup-20260420-140401.sql` 初始化表结构与数据

### 执行结果

- 数据库恢复到干净基线状态
- 种子数据完整写入
- 旧数据全部清空

## 4. data-quality-check.ps1 说明

### 位置

`scripts/ops/data-quality-check.ps1`

### 检查项

| 检查项 | 说明 | 期望结果 |
|--------|------|---------|
| 表计数 | 检查关键表是否为空 | 所有表 > 0 |
| 中文编码 | 抽查中文字段是否正常 | 无乱码 |
| 外键完整性 | 检查关联引用完整性 | 0 孤立记录 |
| 权限数据 | 角色/权限/用户关联完整性 | 4 角色、4 用户、14+ 权限码 |
| 库存一致性 | 库存量与事务记录对比 | 匹配 |
| FAQ/SOP 数据 | 知识库非空 | FAQ ≥ 8、SOP ≥ 3 |
| 规则配置 | 规则表非空 | ≥ 5 条规则 |
| 工单数据 | 客服工单非空 | ≥ 2 条工单 |

### 输出格式

```
=== Data Quality Check: 2026-04-27 09:32:00 ===
[PASS] Table users: 4 rows (expected >= 4)
[PASS] Table roles: 4 rows (expected >= 4)
[PASS] Table permissions: 14 rows (expected >= 14)
[PASS] Chinese encoding: users.nickname OK (admin → 管理员)
[PASS] Chinese encoding: products.name OK (PROD-001 → 螺丝 M8)
[PASS] Foreign key: user_roles → users OK
[PASS] Foreign key: user_roles → roles OK
[PASS] Inventory consistency: OK
[PASS] FAQ: 10 rows (expected >= 8)
[PASS] SOP: 5 rows (expected >= 3)
[PASS] Rule config: 5 rows (expected >= 5)
[PASS] Customer tickets: 2 rows (expected >= 2)
=== Result: ALL PASS ===
```

## 5. 数据问题排查

### 5.1 编码问题

如果发现中文乱码：
1. 检查 `backups/novadepot-backup-*.sql` 文件编码（应为 UTF-8 without BOM）
2. 检查 MySQL 连接参数是否指定 `--default-character-set=utf8mb4`
3. 检查 `CREATE DATABASE` 语句中的 CHARSET/COLLATION 设置
4. 检查 Docker MySQL 容器的 my.cnf 配置

### 5.2 数据丢失

如果 reset 后数据不符合预期：
1. 检查 `backups/` 中的 SQL 文件是否完整
2. 通过 `docker exec` 进入 MySQL 手动检查表数据
3. 对比 SQL 文件与数据库实际内容

### 5.3 权限错误

如果某些用户登录后无权限：
1. 检查 `role_permissions` 关联表
2. 检查 `user_roles` 关联表
3. 对照 `docs/63-v1-roles-and-permissions.md` 验证权限码是否完整