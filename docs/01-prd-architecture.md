# NovaDepot PRD + 架构设计（Java/Spring 版）

## A. 产品定位与目标用户

### 产品定位
NovaDepot 是面向中小商家、仓库、企业内部管理的业务系统，核心是 WMS（仓储管理），并提供轻量 ERP、企业助手、仓库助手、智能客服能力。系统从一开始按 SaaS 商业化标准设计，支持后续多租户和收费运营。

### 目标结果
- 让仓储作业标准化、可追溯、可量化
- 让采购-销售-库存-客服信息形成业务闭环
- 让管理层可通过数据看板做经营决策
- 让系统具备可商用扩展能力（多租户、计费、审计、AI 升级）

### 目标用户画像
- 中小电商商家：多渠道订单与库存协同
- 传统贸易公司：采购、销售、库存、应收应付联动
- 内部物资仓企业：领用、调拨、盘点、审批可追溯
- 客服团队：订单查询、售后处理、统一话术

### 核心价值主张
- 对仓库：提升库存准确率与作业效率
- 对管理：单据全链路可追踪、审计可追责
- 对客服：缩短响应时间，提高处理一致性
- 对企业：低成本上线，后续平滑 SaaS 化

---

## B. 角色体系设计

### 角色层级
- 平台层（商用版）
  - `PLATFORM_OWNER`：平台运营、租户管理、套餐策略
  - `PLATFORM_OPS`：平台运维、监控、客服支持
- 租户层
  - `TENANT_ADMIN`：租户内用户、角色、参数配置
  - `TENANT_AUDITOR`：审计只读与日志追踪
- 业务层
  - `WAREHOUSE_MANAGER`：仓储策略、盘点审批、异常处理
  - `WAREHOUSE_OPERATOR`：入库/出库/盘点执行
  - `PURCHASER`：采购流程与供应商管理
  - `SALES`：销售订单与客户管理
  - `FINANCE`：应收应付、收付款、对账
  - `CUSTOMER_SERVICE`：客服会话、工单处理、知识库维护
  - `VIEWER`：跨模块查看，无编辑权限

### 权限模型（RBAC + 数据范围）
- 功能权限：菜单、接口、按钮级控制
- 数据权限：按仓库、组织、租户、业务线隔离
- 字段权限：成本价、毛利、客户联系方式等敏感字段受控
- 审批权限：提交、审核、驳回、撤回、作废分离

### 审计要求
- 高风险操作必须记录：操作人、时间、原值、新值、IP、终端
- 单据状态流转必须可回放

---

## C. 核心业务模块清单

### 1. WMS 核心模块
- 仓库/库区/库位管理
- 商品主数据（SPU/SKU、条码、单位、批次、序列号）
- 入库管理（采购入库、退货入库、其他入库）
- 出库管理（销售出库、领用出库、其他出库）
- 调拨管理（仓间、库位间）
- 盘点管理（任务、差异、调整）
- 库存引擎（可用库存、冻结库存、在途库存）
- 库存流水（库存变更事实表）

### 2. 轻量 ERP 模块
- 主数据中心（供应商、客户、组织、商品）
- 采购管理（采购申请、采购单、收货关联）
- 销售管理（销售单、发货、退货）
- 财务简化（应收应付、收付款、核销）
- 单据中心（统一编号、状态机、审批流）

### 3. 平台能力模块
- 报表中心（库存、周转、采购、销售、绩效）
- 通知中心（站内信、邮件、短信、Webhook）
- 导入导出中心（Excel/CSV）
- 审计日志中心（操作日志、数据变更日志）
- 文件管理（对象存储）

### 4. 智能模块
- 企业助手（经营问答、报表解释）
- 仓库助手（作业建议、库存预警解释）
- 智能客服（FAQ、会话辅助、工单建议）

---

## D. 分阶段开发路线图（MVP -> 可用版 -> 商用版）

### 阶段 1：MVP（8-12 周）
目标：打通“仓储作业 + 库存正确性 + 基础权限 + 免费 AI 助手”的核心闭环。

交付清单：
- 用户、登录、JWT、基础 RBAC
- 仓库、库位、SKU 主数据
- 入库、出库、盘点、调拨核心流程
- 库存快照 + 库存流水
- 基础报表（库存余额、出入库日报）
- Mock AI + 规则引擎 + 模板回复
- 基础客服（FAQ + 工单）
- Docker Compose 本地一键启动（web/api/mysql/redis）

验收口径：
- 库存账实准确率 >= 98%
- 关键流程可追溯率 100%
- 本地 Docker 环境 15 分钟内可启动

### 阶段 2：可用版（12-20 周）
目标：形成稳定的“WMS + 轻 ERP + 客服”业务协同。

交付清单：
- 采购、销售、应收应付闭环
- 审批流与单据状态机完善
- 字段级审计日志
- 通知中心、导入导出任务化
- 可视化驾驶舱（仓储+经营）
- AI 编排器（规则链 + SQL 模板解释）
- 客服 SLA 与升级策略
- Docker 镜像分环境配置（dev/docker）

验收口径：
- 单据异常可回滚、可重试
- 报表常用查询响应 < 3 秒
- 客服首响效率较原流程提升 >= 30%

### 阶段 3：商用版（20+ 周）
目标：SaaS 化、可计费、多租户、付费 AI 接入。

交付清单：
- 多租户隔离（行级隔离起步）
- 套餐、配额、计费、账单
- AI Provider 扩展接入付费模型 API
- 租户级监控、告警、备份恢复
- 平台运营后台（租户开通、配额管理）
- 桌面环境部署包（含 Docker Compose）

验收口径：
- 租户数据串租风险为 0
- 新租户开通至可登录 < 30 分钟
- AI Provider 切换无需改动业务代码

---

## E. 页面清单与页面职责

### 1. 入口与工作台
- 登录页：账号密码、租户识别、记住设备
- 首页工作台：待办、预警、关键指标卡片

### 2. 仓储模块页面
- 仓库管理页：仓库/库区/库位维护
- 商品主数据页：SKU、条码、批次策略
- 入库单列表/详情：创建、审核、入库确认
- 出库单列表/详情：拣货、复核、出库确认
- 盘点任务页：盘点执行、差异处理
- 调拨页：调出/调入联动
- 库存查询页：可用/冻结/在途库存
- 库存流水页：按单据、SKU、时间追踪

### 3. ERP 模块页面
- 采购管理页：采购单、收货关联
- 销售管理页：销售单、发货、签收
- 客户/供应商页：主数据维护
- 应收应付页：账款登记、核销
- 审批中心页：待审、已审、驳回

### 4. 智能与客服页面
- 企业助手页：经营问题问答、报表解读
- 仓库助手页：作业建议、风险提示
- 客服工作台：会话、建议回复、工单入口
- 知识库页：FAQ、SOP、模板话术管理

### 5. 平台运营页（商用版）
- 租户管理：开通、冻结、配额
- 套餐与账单：订阅、续费、账单明细
- 系统配置：AI Provider、通知通道、对象存储

### 前端实现要求（统一）
- SaaS 风格：简洁、信息密度高、重交互
- 支持深色/浅色模式
- 响应式支持（桌面优先，移动可用）
- 重点页面使用 Framer Motion 与 Recharts/ECharts

---

## F. 核心业务流程

### 流程 1：采购入库
采购申请 -> 采购单审批 -> 到货登记 -> 质检（可选）-> 入库上架 -> 库存增加 -> 记录库存流水 -> 通知采购与财务

### 流程 2：销售出库
销售单创建 -> 库存锁定 -> 拣货 -> 复核 -> 出库确认 -> 库存扣减 -> 物流状态回传 -> 客服侧可查询

### 流程 3：盘点纠偏
创建盘点任务 -> 扫码/录入 -> 差异复核 -> 审批 -> 库存调整单 -> 更新库存快照 -> 审计日志留痕

### 流程 4：客服处理
客户咨询 -> FAQ 命中自动回复 -> 未命中转人工建议回复 -> 必要时转工单 -> 工单结果回写知识库

### 流程 5：AI 问答
用户提问 -> AI 编排器识别意图 -> 路由规则/模板/伪分析 -> 返回答案 + 数据来源 -> 记录会话日志

---

## G. 数据库表设计总览（MySQL 8）

说明：所有业务表建议预留 `tenant_id`、`created_by`、`created_time`、`updated_by`、`updated_time`、`deleted`（逻辑删除）字段。

### 1. 组织与权限
- `sys_tenant`
- `sys_user`
- `sys_role`
- `sys_permission`
- `sys_user_role`
- `sys_role_permission`
- `sys_data_scope`

### 2. 主数据
- `md_warehouse`
- `md_location`
- `md_product`
- `md_sku`
- `md_supplier`
- `md_customer`
- `md_unit`

### 3. 单据与库存
- `wms_inbound_order`
- `wms_inbound_order_item`
- `wms_outbound_order`
- `wms_outbound_order_item`
- `wms_transfer_order`
- `wms_transfer_order_item`
- `wms_stocktake_order`
- `wms_stocktake_order_item`
- `wms_inventory_balance`
- `wms_inventory_ledger`

### 4. ERP 相关
- `erp_purchase_order`
- `erp_purchase_order_item`
- `erp_sales_order`
- `erp_sales_order_item`
- `erp_ar_ap_account`
- `erp_payment_record`

### 5. 平台能力
- `wf_approval_instance`
- `wf_approval_task`
- `sys_notification`
- `sys_import_job`
- `sys_export_job`
- `sys_audit_log`
- `sys_attachment`

### 6. AI 与客服
- `ai_session`
- `ai_message`
- `ai_provider_config`
- `ai_prompt_template`
- `cs_ticket`
- `cs_ticket_message`
- `cs_faq`
- `cs_kb_article`

### 索引与约束建议
- 高频查询联合索引：`tenant_id + biz_no`、`tenant_id + status + created_time`
- 库存核心索引：`tenant_id + warehouse_id + sku_id`
- 库存流水幂等键：`tenant_id + request_id` 唯一
- 金额字段使用 `DECIMAL(18,2)`，库存数量使用 `DECIMAL(18,6)`

---

## H. 后端模块划分（Java 17 + Spring Boot 3）

### 分层建议
- `controller`：API 入参校验、响应封装
- `application`：用例编排、事务边界
- `domain`：领域模型、状态机、规则
- `infrastructure`：MyBatis-Plus、Redis、MQ、外部适配

### 核心业务模块
- `novadepot-auth`：Spring Security、JWT、登录
- `novadepot-iam`：RBAC、数据权限
- `novadepot-masterdata`：主数据管理
- `novadepot-wms-inbound`
- `novadepot-wms-outbound`
- `novadepot-wms-inventory`
- `novadepot-wms-stocktake`
- `novadepot-erp-procurement`
- `novadepot-erp-sales`
- `novadepot-erp-finance-lite`
- `novadepot-workflow`：审批流、状态机
- `novadepot-report`：报表聚合
- `novadepot-notify`：通知适配器
- `novadepot-audit`：审计日志
- `novadepot-ai-core`：AI 编排与 Provider 抽象
- `novadepot-cs`：客服与知识库

### 公共基础模块
- `novadepot-common`：统一异常码、通用 DTO、工具
- `novadepot-starter`：自动配置与组件装配
- `novadepot-job`：定时任务

### 配置体系
- `application.yml`：基础通用配置
- `application-dev.yml`：本地开发配置
- `application-docker.yml`：容器内服务地址与参数

---

## I. AI 模块设计方案（重点）

### 目标
- 开发阶段 0 成本可用
- 商用阶段低成本接入付费模型
- 业务调用层无感知切换

### 核心架构
- `AIProvider`：统一接口
  - `chat(AIRequest request)`
  - `analyze(AIAnalysisRequest request)`
  - `suggest(AISuggestRequest request)`
  - `health()`
- `AIOrchestrator`：根据场景路由到不同 Provider
- `AIPolicyEngine`：权限、敏感词、越权检查
- `PromptTemplateService`：模板管理与版本控制

### Provider 分层
- `MockProvider`：固定回复/脚本回复，支持联调
- `RuleProvider`：规则树 + SQL 聚合解释
- `TemplateProvider`：模板变量填充
- `PaidLLMProvider`（后续）：OpenAI/其他厂商

### 路由策略（建议）
- 客服 FAQ：优先 Rule + Template
- 仓储建议：Rule + 指标阈值判断
- 经营问答：SQL 聚合 + 模板总结
- 复杂开放问答（商用版）：PaidLLMProvider

### 关键开关
- `ai.provider.default=mock`
- `ai.paid.enabled=false`（开发默认）
- `ai.route.customer-service=rule-template`
- `ai.route.enterprise-assistant=rule-sql`

### 兼容原则
- Controller/Service 只能依赖 `AIOrchestrator`
- 禁止业务模块直接依赖某个厂商 SDK
- 新增付费 Provider 仅在 `novadepot-ai-core` 内完成

---

## J. 智能客服模块设计方案

### 功能分层
- 自助层：FAQ 检索 + 模板自动回复
- 辅助层：给坐席生成“建议回复”，人工确认发送
- 工单层：复杂问题流转工单，配置 SLA 与升级策略

### 核心能力
- 上下文聚合：订单、物流、库存、历史会话
- 坐席工作台：会话窗口、用户画像、推荐话术
- 知识闭环：会话高频问题转 FAQ 草稿
- 质检能力（商用版）：响应时效、准确率、合规评分

### MVP 方案（免费 AI）
- FAQ 检索：关键词 + 标签
- 模板回复：按场景参数自动填充
- 规则建议：延迟发货、缺货、退款等触发标准话术
- 工单兜底：未命中 FAQ 自动建单建议

### 商用升级
- 接入向量检索与付费模型
- 引入意图识别与情绪识别
- 引入自动总结与坐席绩效分析

---

## K. 商业化与多租户扩展建议

### 多租户演进路径
- V1：共享库 + `tenant_id` 行级隔离
- V2：高价值租户可切分独立 schema
- V3：超大客户独立实例部署

### 商业化能力
- 套餐分级：基础版、专业版、旗舰版
- 配额控制：用户数、仓库数、单据量、AI 调用量
- 增值服务：高级报表、接口对接、专属客服

### 计费与审计
- 计费事件统一采集：API 调用、导出次数、AI 调用
- 审计与计费同源事件流，确保可对账

### Docker 与桌面部署建议
- 标准本地部署：Docker Compose（`web`、`api`、`mysql`、`redis`、`nginx` 可选）
- 支持离线环境：镜像提前打包导入
- 桌面环境提供“一键启动脚本 + 环境自检脚本”

---

## L. 项目风险与优先级建议

### P0（必须先做）
- 库存准确性与库存流水完整性
- JWT + RBAC + 数据权限
- 单据状态机与审批合法性
- AI Provider 抽象层落地（先 Mock/Rule）
- Docker Compose 本地可直接运行

### P1（强烈建议）
- 字段级审计日志
- 导入导出异步化与失败补偿
- 报表性能优化与缓存策略
- 客服 SLA 规则与知识库闭环

### P2（可延后）
- 高级 BI 大屏
- 插件生态与第三方市场
- 复杂计费模型

### 主要风险与应对
- 风险：前期业务快写导致架构耦合
  - 应对：坚持模块边界 + 领域事件 + 统一状态机
- 风险：AI 接入路径写死
  - 应对：业务仅调用 `AIOrchestrator`，Provider 可插拔
- 风险：多租户后补改造成本高
  - 应对：MVP 即保留 `tenant_id` 与索引规划
- 风险：本地部署复杂，团队无法复现
  - 应对：统一 Docker Compose、健康检查、启动文档

---

## 实施建议（下一步文档）
1. `docs/02-domain-model.md`：领域模型、聚合、状态机与领域事件。
2. `docs/03-api-contracts.md`：MVP 核心接口合同（OpenAPI 粒度）。
3. `docs/04-deployment-docker.md`：Docker Compose 拓扑、环境变量、启动验收清单。
