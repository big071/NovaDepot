# NovaDepot 智能客服模块方案（可商用演进）

## A. 智能客服定位

### 1. 产品定位
NovaDepot 智能客服是“仓储 + 轻 ERP + AI”闭环中的服务中台，覆盖对外客户咨询与对内运营协同两条链路：
- 外部客服：面向客户/商家，解决订单、发货、库存可售、售后、退换货咨询。
- 内部客服：面向员工/运营，承接跨部门问题流转、异常升级、工单追踪与 SLA 管控。

### 2. 目标价值
- 降低人工重复回复比例（FAQ + 规则自动回复）。
- 缩短首响时长与问题解决时长。
- 让客服可直接调用订单/库存数据，减少跨系统查找。
- 形成会话-工单-知识库-绩效的可运营闭环，支持后续 SaaS 商用计费。

---

## B. 适用场景

| 场景 | 外部客服 | 内部客服 | MVP支持 | 商用增强 |
|---|---|---|---|---|
| 常见问题自动回复 | 支持 | 支持 | 是 | 增加多语言与渠道模板 |
| 订单/发货/库存查询 | 支持 | 支持 | 是 | 增加批量查询与预测到货 |
| 售后咨询 | 支持 | 支持 | 是 | 增加自动判责与赔付建议 |
| 退换货说明 | 支持 | 支持 | 是 | 增加政策按租户差异化 |
| 人工介入 | 支持 | 支持 | 是 | 增加智能分配与技能路由 |
| 历史会话追溯 | 支持 | 支持 | 是 | 增加质检抽检与摘要检索 |
| 客服绩效统计 | 间接 | 支持 | 是 | 增加班组/渠道/时段分析 |
| 满意度评价 | 支持 | 支持 | 是 | 增加评价驱动改进闭环 |

---

## C. 会话流程设计

### 1. 外部客服流程
1. 客户发起咨询（Web/小程序/IM 渠道接入）。
2. 系统先走 FAQ 检索与规则回复。
3. 命中失败或置信度低时，调用 AI 客服回复（当前 mock/rule，后期 paid）。
4. 触发人工条件时转接坐席。
5. 若问题无法即时解决，升级工单并跟踪处理。
6. 问题解决后收集满意度并归档知识候选。

### 2. 内部客服流程
1. 员工/运营提交内部咨询或异常。
2. 系统自动关联订单、库存流水、出入库单据。
3. AI 提供处理建议与 SOP。
4. 必要时转指定部门（仓储/采购/销售）。
5. 超时自动升级到主管并触发通知。

---

## D. 会话状态设计

| 状态 | 含义 | 可转移到 | 触发动作 |
|---|---|---|---|
| NEW | 新会话待受理 | BOT_PROCESSING, HUMAN_QUEUE, CLOSED | 创建会话 |
| BOT_PROCESSING | 机器人处理中 | WAITING_USER, HUMAN_QUEUE, TICKET_PENDING | FAQ/规则/AI回复 |
| HUMAN_QUEUE | 待人工接入 | IN_SERVICE, CLOSED | 分配坐席 |
| IN_SERVICE | 人工处理中 | WAITING_USER, TICKET_PENDING, RESOLVED | 人工回复 |
| WAITING_USER | 等待用户反馈 | IN_SERVICE, RESOLVED, CLOSED | 用户超时提醒 |
| TICKET_PENDING | 已升级工单处理中 | IN_SERVICE, RESOLVED | 工单状态联动 |
| RESOLVED | 已解决待评价 | CLOSED, REOPENED | 发送满意度问卷 |
| REOPENED | 用户重新打开 | IN_SERVICE, TICKET_PENDING | 继续处理 |
| CLOSED | 会话关闭 | REOPENED | 归档 |

状态约束：
- 禁止跳级流转。
- 每次状态变更写 `audit_logs`。
- `RESOLVED/CLOSED` 必须记录处理人和处理时长。

---

## E. 消息结构设计

### 1. 消息类型
- `TEXT`：文本
- `RICH_CARD`：结构化卡片（订单状态、库存卡）
- `SYSTEM_EVENT`：系统事件（转接、升级、关闭）
- `AI_SUGGESTION`：AI建议回复

### 2. 建议字段（对应 `customer_service_messages`）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| tenant_id | BIGINT | 租户 |
| session_id | BIGINT | 会话ID |
| sender_type | VARCHAR(16) | CUSTOMER/AGENT/AI/SYSTEM |
| sender_id | BIGINT | 发送人 |
| content | TEXT | 消息内容 |
| msg_type | VARCHAR(16) | TEXT/RICH_CARD/SYSTEM_EVENT |
| ai_suggested | TINYINT | 是否AI建议 |
| ext_json | JSON(建议扩展) | 结构化载荷（订单号、按钮动作） |
| created_at | DATETIME(3) | 发送时间 |

---

## F. FAQ 知识库设计

### 1. 分层结构
- 通用层：平台标准 FAQ（发货、退换货、时效）。
- 租户层：商家自定义 FAQ（政策、售后规则、服务承诺）。
- 场景层：按 `scene`（售前/售后/物流/库存）分类。

### 2. 知识条目（`faq_knowledge`）
- 核心字段：`faq_code, question, answer, tags, scene, priority, enabled, version_no`。
- 检索策略：关键词 + 标签 + 场景优先级。
- 运营机制：命中率低的问题自动进入“待补充”列表。

---

## G. 规则回复设计

### 1. 规则引擎范围（MVP）
- 订单查询：识别订单号，回复状态与预计发货时间。
- 发货查询：识别物流关键词，回复发货节点与异常提示。
- 库存查询：识别 SKU/商品名，回复可售库存与补货提示。
- 退换货说明：按租户政策模板回复。
- 售后咨询：按问题类型给标准处理步骤。

### 2. 规则模型（`customer_service_rules`）
- `trigger_type`：KEYWORD/INTENT/REGEX
- `trigger_expr`：触发表达式
- `action_type`：REPLY_TEMPLATE/ROUTE_AGENT/CREATE_TICKET
- `action_config`：模板ID、队列ID、优先级等配置

---

## H. AI 回复设计（前期免费，后期付费）

### 1. 当前阶段（免费）
- Provider：`MockProvider + RuleProvider`。
- 输出：建议回复 + 置信度 + 引用来源（规则/FAQ/库存查询）。
- 高风险问题（赔付、纠纷）默认触发“建议人工复核”。

### 2. 后期阶段（商用）
- Provider：`PaidLLMProvider` 灰度接入。
- 保持 API 不变：`/api/v1/ai/chat` 与 `/api/v1/customer-service/*`。
- 路由策略：客服场景默认 `rule`，复杂文本场景可路由 `paid`。
- 失败兜底：paid 不可用自动回退 rule/mock。

---

## I. 人工转接机制

### 1. 触发条件
- AI 置信度低于阈值。
- 用户明确要求人工。
- 命中敏感词（投诉、赔偿、法律风险）。
- 多轮未解决（例如 3 轮以上）。

### 2. 分配策略
- 先按技能组（售前/售后/物流）分流。
- 再按在线状态 + 当前负载分配。
- 超过等待阈值自动升级主管。

### 3. 审计要求
- 记录转接发起人、目标坐席、原因、时间。
- 写入 `audit_logs` 与会话系统消息。

---

## J. 工单升级机制

### 1. 升级触发
- 会话超 SLA 未响应。
- 涉及财务损失/退赔。
- 跨部门协同（仓储、采购、销售）处理。

### 2. 工单生命周期（建议）
`OPEN -> ASSIGNED -> PROCESSING -> PENDING_EXTERNAL -> RESOLVED -> CLOSED`

### 3. 与会话联动
- 会话状态进入 `TICKET_PENDING`。
- 工单完成后回写会话并通知客户。

---

## K. 客服工作台页面设计

### 1. 页面结构（Next.js）
- 路由建议：`/cs/workspace`
- 布局：三栏
  - 左：会话列表（状态、优先级、未读）
  - 中：消息区（时间线、快捷回复、AI建议）
  - 右：用户与业务上下文（订单、发货、库存、工单）

### 2. 子页面建议
| 页面 | 路由 | 说明 |
|---|---|---|
| 客服工作台 | `/cs/workspace` | 实时处理会话 |
| 工单中心 | `/cs/tickets` | 升级工单与进度 |
| FAQ/知识库 | `/cs/knowledge` | FAQ维护与发布 |
| 规则配置 | `/cs/rules` | 自动回复规则管理 |
| 客服报表 | `/reports/service` | SLA与绩效分析 |

### 3. 高交互重点
- 会话切换、实时未读、快捷动作（转接/升级/关闭）
- AI 建议插入编辑器（二次编辑后发送）
- 一键查询订单/库存上下文

---

## L. 客服数据统计设计

### 1. 核心指标
| 指标 | 说明 |
|---|---|
| 会话量 | 日/周/月总会话数 |
| 首响时长 FRT | 从创建到首次有效回复 |
| 平均处理时长 AHT | 会话总耗时 |
| 一次解决率 FCR | 无需二次打开的比例 |
| 转人工率 | 机器人转人工占比 |
| 工单升级率 | 会话升级工单比例 |
| 满意度 CSAT | 用户评分均值与分布 |
| 坐席绩效 | 人均处理量、平均时长、满意度 |

### 2. 统计维度
- 租户、渠道、场景、坐席、班组、时段、问题类型。

### 3. 数据来源
- `customer_service_sessions`
- `customer_service_messages`
- `notifications`
- `audit_logs`
- 评价表（建议新增 `customer_service_ratings`）

---

## M. 风险控制建议

### 1. 业务风险
- 错误承诺风险：AI 输出涉及赔付时必须人工确认。
- 越权查询风险：订单/库存查询必须走 RBAC + tenant 过滤。
- 漏处理风险：SLA 预警与自动升级。

### 2. 数据与合规
- 敏感信息脱敏（手机号、地址、证件号）。
- 消息留痕与可追溯审计。
- AI 回复标识清晰（区分 AI/人工）。

### 3. 系统稳定性
- Redis 做会话未读与限流缓存。
- 关键写操作幂等（requestId）。
- 外部渠道异常时支持重试与死信队列（后续扩展）。

---

## N. 商用版本建议

### 1. 套餐能力分层
- Basic：FAQ + 规则回复 + 基础会话工作台。
- Pro：AI建议回复 + 工单升级 + 绩效报表。
- Enterprise：多渠道接入、质检、SLA高级策略、专属模型路由。

### 2. 商用可计费项
- 月活会话数
- 坐席账号数
- AI 调用量（rule 免费，paid 按量）
- 高级报表与导出
- 多渠道与Webhook集成

### 3. 迭代优先级
1. P0：会话、消息、FAQ、规则、人工转接、工单升级、基础报表。
2. P1：满意度评价、绩效看板、多渠道接入、智能分配。
3. P2：客服质检、对话摘要、付费模型增强、多租户运营控制台。

---

## Docker 本地联调与运行约束

### 1. 前后端地址
- 前端：`http://localhost:3000`
- 后端：`http://localhost:8080`
- API Base：`NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1`

### 2. 客服接口联调建议
- 会话列表：`GET /api/v1/customer-service/sessions`
- 消息列表：`GET /api/v1/customer-service/sessions/{id}/messages`
- 消息发送：`POST /api/v1/customer-service/sessions/{id}/messages`
- 人工转接：`POST /api/v1/customer-service/sessions/{id}/actions/transfer-human`
- 工单创建：`POST /api/v1/customer-service/tickets`

### 3. 容器依赖
- `backend` 依赖 `mysql + redis`
- `frontend` 依赖 `backend`
- MySQL 初始化脚本路径：`backend/deploy/mysql/init`

### 4. 开发约束
- 当前按单体模块化实现，后续可拆分 `novadepot-cs`。
- 所有客服查询必须带租户隔离（`tenant_id`）。
- AI 相关走 Provider 抽象层，避免直接绑定厂商 SDK。
