# NovaDepot 权限、工单持久化与详情链路修复说明（2026-04-11）

## 1. 本轮目标
- 回答并修复“权限不足是否有高权限用户”问题。
- 将客服工单从内存 mock 升级为 MySQL 持久化，提供最小分页查询。
- 为商品/库存页面补齐“从仪表盘进入”筛选标签与清除按钮。
- 修复商品、仓库、库位详情链路。
- 修复 AI 会话“会话不存在”问题。

## 2. 问题清单与根因

### 2.1 权限不足
- 当前鉴权为 mock 登录发 token，并非真实 RBAC 用户库校验。
- 结论：当前不存在“比 admin 更高”的真实系统用户分级；`admin/operator/guest` 在当前实现中拿到相同权限集合（32项）。
- 若出现权限不足，优先排查 token 是否为旧 token（未重新登录导致缺新权限）。

### 2.2 工单创建后难回查
- 根因：工单仅存内存，服务重启丢失；查询接口无分页。
- 处理：新增 `customer_service_tickets` 表并接入持久化查询。

### 2.3 商品/仓库/库位详情异常
- 根因：详情按 Long ID 查询，前端 JS number 在大整数场景有精度丢失；库位详情接口此前缺失。
- 处理：详情改为按业务编码查询（productCode / warehouseCode / locationCode），绕开前端大整数精度问题；补齐库位详情接口。

### 2.4 AI 会话不存在
- 根因：会话消息按 Long conversationId 查询，前端 number 精度问题导致 ID 失真。
- 处理：新增按 `conversationNo` 查询与续聊能力，前端改为基于 `conversationNo` 驱动会话。

### 2.5 仪表盘筛选清除缺失
- 根因：已有默认筛选，但缺少显式清除入口。
- 处理：商品页、库存页增加“已应用筛选”标签与“清除筛选”按钮。

## 3. 本轮接口/数据调整

### 3.1 客服工单
- 新表：`customer_service_tickets`
- 创建工单：`POST /api/v1/customer-service/tickets`
- 分页查询：`GET /api/v1/customer-service/tickets?sessionId=&pageNo=&pageSize=`
- 返回结构：`{ list, total, pageNo, pageSize, dataSource }`

### 3.2 详情查询
- 商品详情：新增按编码查询接口。
- 仓库详情：新增按编码查询接口。
- 库位详情：新增按编码查询接口。

### 3.3 AI 会话
- 会话列表保留。
- 新增按 `conversationNo` 查询消息接口。
- chat 入参支持 `conversationNo`，用于续聊已有会话。

## 4. 验收标准
1. 创建工单后可分页回查，且服务重启后数据仍在。
2. 商品/仓库/库位详情稳定可用。
3. AI 会话切换与历史回放正常，不再出现“会话不存在”。
4. 商品/库存页可一键清除仪表盘筛选条件。
5. Docker 本地运行兼容。

## 5. 本地升级说明
1. 若数据库已初始化，请执行新增 SQL：
   - `docker compose exec mysql mysql -uroot -proot novadepot < /docker-entrypoint-initdb.d/03-schema-erp-system-ai-cs.sql`
2. 该脚本中工单表使用 `CREATE TABLE IF NOT EXISTS`，可重复执行。

## 6. 本轮实现结果
1. 客服工单：
   - 数据表 `customer_service_tickets` 已落地。
   - `POST /api/v1/customer-service/tickets` 已落库（MySQL）。
   - `GET /api/v1/customer-service/tickets` 已支持 `sessionId + pageNo + pageSize` 分页返回。
2. 详情链路：
   - 商品详情新增 `GET /api/v1/products/code/{productCode}`。
   - 仓库详情新增 `GET /api/v1/warehouses/code/{warehouseCode}`。
   - 库位详情新增 `GET /api/v1/locations/code/{locationCode}`。
   - 前端三页详情按钮已改为按编码查询，规避前端大整数精度问题。
3. AI 会话：
   - 新增 `GET /api/v1/ai/conversations/by-no/{conversationNo}/messages`。
   - chat 支持传 `conversationNo` 续聊。
   - 前端 AI 会话切换与历史加载改为 `conversationNo` 驱动。
4. 仪表盘筛选清除：
   - 商品页新增“已应用筛选”提示与“清除筛选”按钮。
   - 库存页新增“已应用筛选”提示与“清除筛选”按钮。

## 7. 验证结果
1. `docker compose up --build -d`：通过。
2. 接口冒烟：
   - 商品/仓库/库位按编码详情接口返回成功。
   - AI 会话创建、按 `conversationNo` 续聊、历史查询成功。
   - 工单分页查询成功（`pageSize=2` 时第一页 2 条、第二页 1 条，`dataSource=MYSQL`）。
