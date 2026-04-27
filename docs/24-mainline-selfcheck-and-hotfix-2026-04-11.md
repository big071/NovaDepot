# NovaDepot 主链路自检与热修复记录（2026-04-11）

## 1. 目标
- 按“测试与修复现有主链路，不新增大功能”执行一轮完整自检。
- 优先修复影响使用的高优先级问题（接口报错、主流程中断）。

## 2. 自检范围
- 登录
- 仪表盘
- 商品
- 仓库
- 库位
- 库存
- 入库
- 出库
- AI 助手
- 客服工作台

## 3. 自检结果

### 3.1 当前可用功能
- 登录：`POST /api/v1/auth/login` 正常返回 token。
- 仪表盘：`GET /api/v1/reports/dashboard` 正常。
- 商品：列表、创建接口正常。
- 仓库：列表、创建接口正常。
- 库位：列表、创建接口正常。
- 库存：库存列表、低库存预警接口正常。
- AI 助手：会话列表、发送消息接口正常（Rule/Mock 路径）。
- 客服工作台：会话、消息、发送、转人工、建工单、FAQ 接口正常。

### 3.2 当前不可用功能
- 入库：创建接口失败（500）。
- 出库：创建接口失败（500）。

### 3.3 报错点清单
- `POST /api/v1/inbound-orders` -> 500（`SYS-9999`）。
- `POST /api/v1/outbound-orders` -> 500（`SYS-9999`）。
- 根因：`inbound_order_items` 与 `outbound_order_items` 表缺少 `location_id` 字段，和后端实体/写入逻辑不一致。

## 4. 修复方案（本轮）
- 在 `backend/deploy/mysql/init/02-schema-wms.sql` 中：
1. 为 `inbound_order_items` 建表定义补充 `location_id`。
2. 为 `outbound_order_items` 建表定义补充 `location_id`。
3. 增加幂等 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS location_id`，兼容已初始化环境。

## 5. 验收标准
- 入库创建接口恢复成功（`code=0`）。
- 出库创建接口恢复成功（`code=0`）。
- 入库审核/过账链路可继续执行。
- 出库审核/发运链路可继续执行。
- Docker Compose 本地运行链路不受影响。
