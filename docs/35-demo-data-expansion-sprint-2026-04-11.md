# NovaDepot 演示数据扩充与成品化推进 Sprint（2026-04-11）

## 1. 目标
- 将当前演示数据从“最小可用”升级为“可真实演示”规模。
- 保持接口契约不变，仅补齐可重复执行的 MySQL 种子数据。

## 2. 当前数据缺口
1. SKU 规模偏小，无法模拟真实筛选、分页与跨仓展示。
2. 每仓库位数量不足，难以体现复杂仓内结构。
3. 入库/出库单据样本不足，多日期与状态分布不充分。
4. 客服与 AI 样本量偏小，典型场景覆盖不完整。
5. 演示角色缺少“仓库主管”层级。
6. 当前 `products` 无独立价格列，演示价格信息需以 `spec` 文案承载（不改表结构）。

## 3. 本轮扩充规模
1. 商品：48 SKU（多分类、规格、建议价文案）。
2. 仓库：3 个。
3. 库位：每仓 16 个，共 48。
4. 库存：多仓分布，覆盖低库存、零库存、高库存。
5. 入库单：24（`DRAFT/SUBMITTED/APPROVED/POSTED`）。
6. 出库单：24（`DRAFT/SUBMITTED/APPROVED/SHIPPED`）。
7. 客服：12 会话 / 60 消息 / 15 工单 / 24 FAQ。
8. AI：12 会话 / 72 消息（库存分析、补货建议、SOP）。
9. 用户角色：admin、仓库主管、仓库员、客服、只读。

## 4. 修改文件清单
1. `backend/deploy/mysql/init/99-seed-mvp.sql`
2. `backend/deploy/mysql/init/97-reset-demo-data.sql`
3. `docs/35-demo-data-expansion-sprint-2026-04-11.md`

## 5. 初始化脚本清单
- 结构脚本：
  - `backend/deploy/mysql/init/01-schema-iam-master.sql`
  - `backend/deploy/mysql/init/02-schema-wms.sql`
  - `backend/deploy/mysql/init/03-schema-erp-system-ai-cs.sql`
- 数据脚本：
  - `backend/deploy/mysql/init/99-seed-mvp.sql`
- 重置脚本：
  - `backend/deploy/mysql/init/97-reset-demo-data.sql`

## 6. 重置开发演示数据方法
方法 A（全量重建）：
1. `docker compose down -v`
2. `docker compose up --build -d`

方法 B（就地重置）：
1. `Get-Content backend/deploy/mysql/init/97-reset-demo-data.sql | docker compose exec -T mysql mysql -uroot -proot novadepot`
2. `Get-Content backend/deploy/mysql/init/99-seed-mvp.sql | docker compose exec -T mysql mysql -uroot -proot novadepot`

## 7. 验收标准
1. 仪表盘与关键模块（商品、库存、入库、出库、AI、客服）默认非空。
2. 数据命名中文清晰，无乱码与占位名。
3. 单据状态、库存状态分布合理，具备真实业务感。
4. 脚本可重复执行，兼容 Docker 本地运行。

## 8. 备注
- 本轮只补数据，不做无关重构。
- 若后续需要更大规模（100+ SKU、千级单据）建议追加 `benchmark` 数据脚本并与默认演示脚本分离。

## 9. 本轮实际落地结果
1. SKU：48 条（满足 30~100 区间）。
2. 仓库：3 个。
3. 库位：48 个（每仓 16 个）。
4. 库存：84 条（覆盖正常、低库存、零库存、高库存、多仓分布）。
5. 入库单：24 条（`DRAFT/SUBMITTED/APPROVED/POSTED` 分布，含多日期）。
6. 出库单：24 条（`DRAFT/SUBMITTED/APPROVED/SHIPPED` 分布，含多日期）。
7. 客服：12 会话、60 消息、15 工单、24 FAQ。
8. AI：12 会话、72 消息（库存分析、补货建议、SOP 场景）。
9. 用户与角色：5 用户（admin、warehouse_manager、operator、cs_agent、viewer）+ 5 角色。

## 10. 验证结果
1. 表数据计数（tenant_id=1）满足非空与规模要求：
   - `products=48`
   - `warehouses=3`
   - `warehouse_locations=48`
   - `inventory=84`
   - `inbound_orders=24`
   - `outbound_orders=24`
   - `customer_service_sessions=12`
   - `customer_service_messages=60`
   - `customer_service_tickets=15`
   - `faq_knowledge=24`
   - `ai_conversations=12`
   - `ai_messages=72`
2. 主链路 API 非空验证通过：
   - `/products`、`/inventory`、`/inbound-orders`、`/outbound-orders`、`/ai/conversations`、`/customer-service/sessions`、`/customer-service/tickets`、`/customer-service/faq`
3. 仪表盘接口非空（有统计值）。

## 11. 下一阶段建议（按优先级）
1. 稳定化继续项（P1）：
   - 权限拒绝（403）场景页面内专属提示统一化。
   - 列表动作后的状态同步（避免长列表局部状态错位）。
2. 自动化测试（P1/P2）：
   - 后端 API 冒烟：登录、入库/出库状态流转、工单创建与分页、AI 会话读取。
   - 前端 E2E：仪表盘跳转、入出库创建+动作、客服发送+建单+分页回查。
3. UI 持续提升（P2）：
   - 仓库页、库位页、登录页对齐当前产品化视觉基线。
   - 状态提示条、空态文案、按钮反馈进一步统一。
4. 性能优化（P2）：
   - 前端路由懒加载与分包（降低主包体积告警）。
   - 列表分页与查询参数缓存优化（避免重复请求）。
5. 后置扩展模块（P3，最后做）：
   - 多租户高级能力、计费商业化、复杂审批编排、外部系统深度集成。
