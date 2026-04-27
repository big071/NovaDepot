# NovaDepot 编码、工单链路与 KPI 跳转修复说明（2026-04-11）

## 1. 目标
- 修复仓库名称与库位名称乱码。
- 修复“工单创建后找不到”问题。
- 为仪表盘 4 个 KPI 卡片增加可点击跳转。

## 2. 问题定位

### 2.1 仓库/库位名称乱码
- 现象：`warehouses.warehouse_name`、`warehouse_locations.location_name` 部分数据显示为 `????`。
- 排查结果：
  1. 数据库、表、字段 charset/collation 均为 `utf8mb4`，结构层正确。
  2. 历史数据已写成 `?`（不可逆脏数据），属于数据层问题，不是单纯前端渲染问题。
  3. JDBC 连接参数需显式加固，最终采用 `characterEncoding=UTF-8` + `connectionCollation=utf8mb4_0900_ai_ci`，避免连接器对 `utf8mb4` 编码名兼容差异导致连接失败。
  4. 初始化 SQL 采用 `SET NAMES utf8mb4`，本轮补充“可重复执行数据修复脚本”。

### 2.2 工单创建后找不到
- 现象：客服页面可创建工单，但后续无法查询或展示。
- 排查结果：
  1. `POST /api/v1/customer-service/tickets` 为 mock 返回。
  2. 无工单查询接口与页面展示入口。
  3. 属于“mock 限制 + 查询缺失”，非纯权限故障。

### 2.3 KPI 卡片不可跳转
- 现象：仪表盘 4 张卡片不可点击。
- 处理策略：增加卡片级路由映射与 query 条件。

## 3. 数据修复策略（可重复执行）

### 3.1 脏数据修复范围
- 表：`warehouses`、`warehouse_locations`
- 字段：`warehouse_name`、`location_name`

### 3.2 修复原则
1. 仅修复命中乱码特征的数据（包含 `?` 的名称）。
2. 不覆盖正常中文数据。
3. 修复结果可重复执行（幂等）。

### 3.3 修复结果格式
- 仓库名：`仓库-<warehouse_code>`
- 库位名：`库位-<location_code>`

## 4. 工单最小可用策略
1. 保留当前客服模块 mock 属性并明确标记。
2. 新增工单查询接口（支持按 `sessionId` 查询）。
3. 客服工作台增加“当前会话关联工单”展示区，创建后即时可见。
4. 权限规则：
   - 创建工单：`CS_TICKET_CREATE`
   - 查询工单：`CS_TICKET_READ`

## 5. KPI 跳转映射
| 卡片 | 含义 | 跳转页面 | Query 参数 | 默认筛选 |
|---|---|---|---|---|
| SKU 总量 | 商品主数据总数 | `/wms/products` | `from=dashboard&metric=sku` | 全量商品 |
| 今日入库单 | 当日入库创建数 | `/wms/inbound` | `from=dashboard&time=today` | 今日创建 |
| 今日出库单 | 当日出库创建数 | `/wms/outbound` | `from=dashboard&time=today` | 今日创建 |
| 低库存预警 | 低库存项数量 | `/wms/inventory` | `from=dashboard&focus=low-stock` | 低库存关注 |

## 6. 验收标准
1. 仓库/库位名称乱码修复后显示正常。
2. 创建工单后可在系统中找到（会话关联工单可见）。
3. 4 张 KPI 卡片可点击并跳转到语义一致页面。
4. Docker 构建运行通过，核心链路无回归。

## 7. 本地数据修复执行说明（可重复）
1. 启动本地 MySQL 容器后执行：
   - `docker compose exec mysql mysql -uroot -proot novadepot < /docker-entrypoint-initdb.d/98-data-repair-warehouse-location-encoding.sql`
2. 该脚本仅修复命中 `?` 的脏数据，不覆盖正常中文名称，可重复执行。
3. 验证 SQL：
   - `SELECT id, warehouse_code, warehouse_name FROM warehouses ORDER BY id;`
   - `SELECT id, location_code, location_name FROM warehouse_locations ORDER BY id;`

## 8. 路由筛选行为说明
1. 仪表盘 `今日入库单` 跳转：`/wms/inbound?from=dashboard&time=today`
   - 入库页默认只展示今日创建单据，并显示“已应用筛选”提示。
2. 仪表盘 `今日出库单` 跳转：`/wms/outbound?from=dashboard&time=today`
   - 出库页默认只展示今日创建单据，并显示“已应用筛选”提示。
3. 仪表盘 `低库存预警` 跳转：`/wms/inventory?from=dashboard&focus=low-stock`
   - 库存页主列表默认聚焦低库存记录，并显示“已应用筛选”提示。
