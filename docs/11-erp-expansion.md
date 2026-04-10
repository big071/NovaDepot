# NovaDepot ERP 核心模块扩展方案

## 范围说明
本文聚焦库存核心闭环之后的 ERP 扩展模块：
- 采购管理
- 销售管理
- 调拨管理
- 盘点管理
- 供应商管理
- 客户管理
- 报表中心

目标：面向中小企业，功能轻量但闭环完整，兼容商用 SaaS 演进，严格复用现有架构（Java + Spring Boot + MySQL + Redis + Docker）。

---

## A. 每个模块的业务目标
| 模块 | 业务目标（实用优先） |
|---|---|
| 采购管理 | 标准化“采购下单-到货-入库”流程，降低缺货与采购失控 |
| 销售管理 | 标准化“销售下单-发货-出库”流程，提升履约效率 |
| 调拨管理 | 管理仓间/库位间库存迁移，减少仓储不平衡 |
| 盘点管理 | 形成“盘点-差异-调整”闭环，保障库存账实一致 |
| 供应商管理 | 管理供应商主数据和合作状态，支撑采购分析 |
| 客户管理 | 管理客户主数据和服务信息，支撑销售分析 |
| 报表中心 | 提供经营与仓储关键报表，支撑日常决策 |

---

## B. 每个模块的核心数据结构
| 模块 | 核心表 | 关键字段（MVP） |
|---|---|---|
| 采购管理 | `purchase_orders`, `purchase_order_items` | `purchase_no`,`supplier_id`,`warehouse_id`,`status`,`total_amount`,`expected_arrival_date` |
| 销售管理 | `sales_orders`, `sales_order_items` | `sales_no`,`customer_id`,`warehouse_id`,`status`,`total_amount`,`delivery_date` |
| 调拨管理 | `transfer_orders`, `transfer_order_items` | `transfer_no`,`from_warehouse_id`,`to_warehouse_id`,`status`,`completed_at` |
| 盘点管理 | `stocktake_orders`, `stocktake_order_items` | `stocktake_no`,`warehouse_id`,`scope_type`,`status`,`diff_count` |
| 供应商管理 | `suppliers` | `supplier_code`,`supplier_name`,`contact_name`,`phone`,`status` |
| 客户管理 | `customers` | `customer_code`,`customer_name`,`contact_name`,`phone`,`status` |
| 报表中心 | 聚合查询 + 导出任务表（后续） | `report_type`,`filters`,`generated_at`,`file_id` |

通用字段统一：
- `id`,`tenant_id`,`created_at`,`updated_at`,`created_by`,`updated_by`,`deleted`

---

## C. 每个模块的状态流转
| 模块 | 状态流转（建议） |
|---|---|
| 采购管理 | `DRAFT -> SUBMITTED -> APPROVED -> RECEIVING -> COMPLETED -> CANCELED` |
| 销售管理 | `DRAFT -> SUBMITTED -> APPROVED -> PICKING -> SHIPPED -> COMPLETED -> CANCELED` |
| 调拨管理 | `DRAFT -> SUBMITTED -> APPROVED -> DISPATCHED -> RECEIVED -> COMPLETED -> CANCELED` |
| 盘点管理 | `DRAFT -> IN_PROGRESS -> DIFF_PENDING -> APPROVED -> ADJUSTED -> COMPLETED -> CANCELED` |
| 供应商管理 | `ACTIVE / INACTIVE` |
| 客户管理 | `ACTIVE / INACTIVE` |
| 报表中心 | `GENERATING -> READY -> FAILED`（导出任务） |

状态约束：
- 禁止跳级流转。
- 流转动作必须落审计日志。

---

## D. 与库存模块的关系
| 模块 | 对库存影响 |
|---|---|
| 采购管理 | 到货入库完成后，触发库存增加（通过入库单，不直改库存） |
| 销售管理 | 发货出库完成后，触发库存减少（通过出库单，不直改库存） |
| 调拨管理 | 调出扣减 + 调入增加，必须双边一致 |
| 盘点管理 | 差异审批后生成调整，触发库存修正 |
| 供应商/客户 | 不直接改库存，作为单据维度来源 |
| 报表中心 | 只读库存快照和流水，不写库存 |

强规则：
- 库存写操作仅通过库存领域服务入口。
- 每次变化写 `inventory_transactions`。

---

## E. 与审计日志的关系
| 模块 | 必记审计动作 |
|---|---|
| 采购管理 | 创建、提交、审核、到货、完成、作废 |
| 销售管理 | 创建、提交、审核、发货、完成、作废 |
| 调拨管理 | 创建、审核、调出确认、调入确认、完成 |
| 盘点管理 | 创建、开始盘点、差异审核、生成调整 |
| 供应商/客户 | 创建、修改、启停用 |
| 报表中心 | 导出任务发起、下载、失败 |

审计字段建议：
- `module`,`action`,`resource_type`,`resource_id`,`before_json`,`after_json`,`operator_id`,`occurred_at`

---

## F. 与通知系统的关系
| 触发事件 | 接收角色 | 通知内容建议 |
|---|---|---|
| 单据待审核 | 审批人 | 单据编号、发起人、金额/数量摘要 |
| 采购到货异常 | 采购员/仓库经理 | 到货差异、缺货项 |
| 出库异常 | 销售/仓库经理 | 库存不足、发货延迟 |
| 盘点差异待审 | 仓库经理/审计员 | 差异数量、影响 SKU |
| 低库存预警 | 采购/仓库经理 | SKU、当前库存、安全阈值 |
| 报表导出完成 | 发起人 | 下载入口、生成时间 |

渠道策略：
- MVP：站内通知
- P1：邮件/Webhook（可选）

---

## G. 前端页面建议
| 模块 | 核心页面 | 页面重点 |
|---|---|---|
| 采购管理 | `/erp/purchases`, `/erp/purchases/new`, `/erp/purchases/[id]` | 列表筛选、到货联动、状态动作 |
| 销售管理 | `/erp/sales`, `/erp/sales/new`, `/erp/sales/[id]` | 履约状态、库存占用提示 |
| 调拨管理 | `/wms/transfers`, `/wms/transfers/new`, `/wms/transfers/[id]` | 调出/调入双状态跟踪 |
| 盘点管理 | `/wms/stocktakes`, `/wms/stocktakes/new`, `/wms/stocktakes/[id]` | 差异高亮、审核入口 |
| 供应商管理 | `/erp/suppliers`, `/erp/suppliers/[id]` | 主数据维护 + 历史汇总 |
| 客户管理 | `/erp/customers`, `/erp/customers/[id]` | 主数据维护 + 订单汇总 |
| 报表中心 | `/reports`, `/reports/wms`, `/reports/purchases`, `/reports/sales` | 可视化 + 导出 |

交互建议：
- 单据详情页使用独立页面，不用弹窗。
- 轻量编辑（启停用/备注）可抽屉。

---

## H. 后端 API 建议
路径统一 `/api/v1/*`，动作统一 `actions` 子路径。

| 模块 | MVP 接口建议 |
|---|---|
| 采购管理 | `GET/POST /purchase-orders`，`POST /purchase-orders/{id}/actions/submit`，`POST /purchase-orders/{id}/actions/approve` |
| 销售管理 | `GET/POST /sales-orders`，`POST /sales-orders/{id}/actions/submit`，`POST /sales-orders/{id}/actions/approve` |
| 调拨管理 | `GET/POST /transfer-orders`，`POST /transfer-orders/{id}/actions/approve`，`POST /transfer-orders/{id}/actions/dispatch`，`POST /transfer-orders/{id}/actions/receive` |
| 盘点管理 | `GET/POST /stocktake-orders`，`POST /stocktake-orders/{id}/actions/count`，`POST /stocktake-orders/{id}/actions/approve-diff`，`POST /stocktake-orders/{id}/actions/adjust` |
| 供应商管理 | `GET/POST/PUT /suppliers` |
| 客户管理 | `GET/POST/PUT /customers` |
| 报表中心 | `GET /reports/dashboard`，`GET /reports/inventory`，`GET /reports/purchase`，`GET /reports/sales`，`POST /reports/exports` |

API 规则：
- 所有写接口支持 `requestId`（幂等预留）。
- 统一响应结构 `ApiResponse`。

---

## I. 开发优先级
| 优先级 | 模块 | 原因 |
|---|---|---|
| P0 | 采购管理、销售管理、调拨管理、盘点管理 | 直接影响库存闭环完整性 |
| P1 | 供应商管理、客户管理 | 保障单据质量与统计维度 |
| P1 | 报表中心（基础） | 经营可视化与管理决策必需 |
| P2 | 报表导出增强、复杂统计 | 商用增强项 |

建议迭代顺序：
1. 采购/销售与入出库联动  
2. 调拨与盘点闭环  
3. 供应商/客户完善  
4. 报表与导出增强

---

## Docker 本地联调与运行建议
1. 后端继续使用 `backend/docker-compose.backend.yml`（backend + mysql + redis）。
2. 前端 `NEXT_PUBLIC_API_BASE_URL` 指向后端 `http://localhost:8080/api/v1`。
3. MySQL 初始化脚本使用 `backend/deploy/mysql/init` 目录自动加载。
4. 新增 ERP 表优先追加 SQL 文件，不改历史脚本（后续迁移工具接管）。

## 与既有架构兼容说明
- 兼容库存闭环：库存统一经库存服务写入 + 流水强制记录。
- 兼容权限模型：沿用 Spring Security + JWT + RBAC + `@RequirePermission`。
- 兼容审计：关键动作统一写 `audit_logs`。
- 兼容 AI 抽象：不直接依赖具体 AI 厂商，保留 `AIProvider` 路由。
