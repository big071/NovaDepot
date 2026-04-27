# NovaDepot 演示数据与种子数据完善 Sprint（2026-04-11）

## 1. 背景与目标
- 目标：系统在默认初始化后即可直接演示，不依赖手工操作造数据。
- 范围：仅补齐 MySQL 种子数据与说明，不做无关重构。
- 约束：兼容 Docker Compose 本地启动。

## 2. 当前缺口盘点
尽管当前数据库中多数表“非空”，但稳定可重复初始化仍有缺口：
1. `99-seed-mvp.sql` 当前稳定覆盖主要是 IAM + FAQ。
2. WMS（仓库/库位/商品/库存/入库/出库）演示数据缺少“脚本级幂等保证”，重置后不一定恢复。
3. 客服（sessions/messages/tickets）与 AI（conversations/messages）示例数据缺少固定种子来源。
4. 仪表盘虽可显示，但趋势与结构化业务样本不足，演示一致性不稳定。

## 3. 种子数据方案（本轮）
按“固定主键 + 幂等 upsert”写入以下模块：
1. IAM：`tenants/users/roles/permissions`（补充多角色演示用户）。
2. WMS 主数据：`warehouses/warehouse_locations/products`。
3. WMS 业务数据：`inventory/inbound_orders/outbound_orders`（含多状态、多日期分布）。
4. 客服：`customer_service_sessions/customer_service_messages/customer_service_tickets`。
5. 知识库：`faq_knowledge`（补充更贴近仓储场景问答）。
6. AI：`ai_conversations/ai_messages`（示例企业问答、库存分析对话）。

补充策略：
- 关键列表默认非空（商品、库存、入库、出库、客服会话、工单、FAQ、AI 会话）。
- 状态覆盖示例：
  - 入库：`DRAFT/SUBMITTED/APPROVED/POSTED`
  - 出库：`DRAFT/SUBMITTED/APPROVED/SHIPPED`
  - 工单：`OPEN/PROCESSING/CLOSED`
- 仓库、库位、SKU 名称采用可读中文命名，避免乱码与占位名。

## 4. 本轮计划修改文件
1. `backend/deploy/mysql/init/99-seed-mvp.sql`（补全演示种子）
2. `backend/deploy/mysql/init/97-reset-demo-data.sql`（新增：一键清空并重灌演示数据）
3. `docs/33-demo-seed-data-sprint-2026-04-11.md`（本说明文档）

## 5. 初始化脚本清单
1. 结构脚本：
   - `backend/deploy/mysql/init/01-schema-iam-master.sql`
   - `backend/deploy/mysql/init/02-schema-wms.sql`
   - `backend/deploy/mysql/init/03-schema-erp-system-ai-cs.sql`
2. 数据脚本：
   - `backend/deploy/mysql/init/99-seed-mvp.sql`
3. 重置脚本（本轮新增）：
   - `backend/deploy/mysql/init/97-reset-demo-data.sql`

## 6. 重置数据方法（开发演示）
方法 A（全量重建，最干净）：
1. `docker compose down -v`
2. `docker compose up --build -d`

方法 B（仅重置演示数据，不销毁卷）：
1. `docker compose exec -T mysql mysql -uroot -proot novadepot < /docker-entrypoint-initdb.d/97-reset-demo-data.sql`
2. `docker compose exec -T mysql mysql -uroot -proot novadepot < /docker-entrypoint-initdb.d/99-seed-mvp.sql`

## 7. 验收标准
1. 默认启动后，以下模块列表均非空：
   - 仪表盘、商品、库存、入库、出库、客服会话、客服工单、FAQ、AI 会话。
2. 仓库/库位/SKU/库存数量命名与数值具备业务合理性。
3. 入库/出库状态分布合理，演示可覆盖草稿到完成流程。
4. 客服与 AI 均有历史会话与消息示例内容。
5. 方案可重复执行（幂等或可重置），兼容 Docker 本地运行。

## 8. 开发演示数据说明
- 本批数据为“开发演示样本”，用于联调、演示与回归，不代表生产策略。
- 所有样本默认归属于 `tenant_id=1`（`tenant_code=default`）。
- 若需扩展多租户演示，建议复制相同结构并替换 `tenant_id/tenant_code`，避免混用同一主键区间。
