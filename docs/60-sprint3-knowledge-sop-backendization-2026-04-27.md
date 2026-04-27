# Sprint 3：知识库与 SOP 后台化落地说明

## 目标

Sprint 3 只做 FAQ、SOP、规则配置的轻量后台化，让客服 AI、AI 助手、Agent 任务可以引用同一套知识来源，并能向业务用户展示可理解的引用依据。

本轮不做向量数据库、RAG 平台、知识图谱、复杂规则引擎、版本审批系统、多租户、计费或新业务模块。

## 当前问题

1. FAQ 已有 `faq_knowledge` 表和客服接口，但维护字段、启停、草稿确认和引用来源表达不完整。
2. SOP 仍主要散落在客服建议、AI 回复、Agent 结果和文档文本中，缺少统一维护表与启停状态。
3. 低库存阈值、自动回复优先级、工单分类关键词、候选回复优先级、Agent 展示阈值存在代码常量或隐式规则。
4. 客服 AI、AI 助手、Agent 结果对知识命中缺少统一的业务依据输出。
5. 高频问题和常见流程缺少“草稿生成 -> 人工确认 -> 启用”的沉淀闭环。
6. 知识变更、规则变更、草稿确认需要进入审计中心，方便按业务编号回查。

## 严格范围

### FAQ 后台化

- 支持问题、答案、场景、标签、优先级、启用/停用状态。
- 支持草稿状态，草稿必须人工确认后才可启用。
- 客服 AI、AI 助手、Agent 命中 FAQ 时展示 FAQ 标题、命中标签和适用场景。

### SOP 后台化

- 新增 SOP 知识维护能力。
- 支持 SOP 标题、适用场景、标准处理步骤、风险点、复核项、启用/停用状态。
- 支持草稿状态，草稿必须人工确认后才可启用。
- 客服工单、物流催发、低库存、异常巡检等场景可引用 SOP。

### 规则配置化

- 低库存默认阈值。
- 自动回复优先级。
- 工单分类关键词。
- 客服候选回复优先级。
- Agent 任务结果展示阈值。

配置采用最小可用表，不提供脚本化规则语言或复杂规则设计器。配置变更必须记录审计。

### 知识沉淀闭环

- 客服工单可生成 FAQ 草稿。
- 客服工单可生成 SOP 草稿。
- 管理员确认后启用。
- AI 不允许自动生成并直接生效。
- 草稿生成和确认启用都要记录审计。

### 角色权限

- 管理员：维护 FAQ、SOP、规则配置，确认草稿启用。
- 客服运营：创建和维护 FAQ/SOP 草稿，但不能直接启用。
- 仓储运营：查看仓储相关 SOP。
- 观察员：只读。

## 不做内容

1. 不做向量数据库。
2. 不做复杂 RAG 平台。
3. 不做知识图谱。
4. 不做复杂规则引擎。
5. 不做版本审批系统。
6. 不接入付费大模型作为强依赖。
7. 不扩展多租户。
8. 不扩展计费。
9. 不新增采购、销售、财务模块。
10. 不重做前端设计系统。

## 后端设计

### 数据表

- 复用并补强 `faq_knowledge`。
- 新增 `sop_knowledge`。
- 新增 `rule_configs`。

FAQ 与 SOP 均采用 `review_status` 表达草稿与已确认状态：

- `DRAFT`：草稿，不参与 AI/客服/Agent 正式命中。
- `APPROVED`：已确认，可在启用后参与命中。

启用状态由 `enabled` 控制。只有 `review_status = APPROVED` 且 `enabled = 1` 的知识会作为正式知识被引用。

### API

- `GET /api/v1/knowledge/faqs`
- `POST /api/v1/knowledge/faqs`
- `PUT /api/v1/knowledge/faqs/{id}`
- `POST /api/v1/knowledge/faqs/{id}/confirm`
- `POST /api/v1/knowledge/faqs/{id}/enable`
- `POST /api/v1/knowledge/faqs/{id}/disable`
- `GET /api/v1/knowledge/sops`
- `POST /api/v1/knowledge/sops`
- `PUT /api/v1/knowledge/sops/{id}`
- `POST /api/v1/knowledge/sops/{id}/confirm`
- `POST /api/v1/knowledge/sops/{id}/enable`
- `POST /api/v1/knowledge/sops/{id}/disable`
- `GET /api/v1/knowledge/rules`
- `PUT /api/v1/knowledge/rules/{configKey}`
- `POST /api/v1/knowledge/drafts/from-ticket/{ticketId}/faq`
- `POST /api/v1/knowledge/drafts/from-ticket/{ticketId}/sop`

### 引用来源

统一返回 `knowledgeRefs`，每条引用至少包含：

- `type`：FAQ、SOP 或 RULE。
- `title`：业务标题。
- `scene`：适用场景。
- `matchedTags`：命中标签。
- `reason`：推荐原因。
- `nextAction`：下一步建议。

## 前端设计

- 在现有导航中增加轻量知识维护入口，不新增复杂知识管理平台。
- 管理员和客服运营可以进入 FAQ/SOP 草稿维护；仓储运营和观察员只读。
- 客服工作台展示 FAQ/SOP/规则引用来源，并提供从工单沉淀 FAQ/SOP 草稿的入口。
- AI 助手回答展示知识引用来源。
- Agent 结果展示“业务依据”，说明使用了哪些 FAQ、SOP 或规则。

## 审计口径

以下动作记录审计：

- FAQ 创建、编辑、启用、停用、确认。
- SOP 创建、编辑、启用、停用、确认。
- 规则配置更新。
- 从工单生成 FAQ 草稿。
- 从工单生成 SOP 草稿。

审计资源类型：

- `FAQ_KNOWLEDGE`
- `SOP_KNOWLEDGE`
- `RULE_CONFIG`
- `CS_TICKET`

## 验收标准

1. FAQ 可以通过后台维护，不再只能依赖 SQL 或代码。
2. SOP 可以通过后台维护，并能被客服 AI、AI 助手或 Agent 结果引用。
3. 客服高频问题可生成 FAQ 草稿，人工确认后启用。
4. 常见处理流程可生成 SOP 草稿，人工确认后启用。
5. 低库存阈值、自动回复优先级、工单分类关键词等至少部分规则可配置。
6. AI、客服、Agent 命中 FAQ 或 SOP 时能显示引用来源。
7. 知识变更和配置变更有审计记录。
8. 角色权限不回退。
9. Docker 本地运行不受影响。
10. E2E 和数据质量检查不回退。

## 后续待办

以下内容不进入 Sprint 3，可进入 v1.1 或 v2 Agent Edition：

1. 知识版本审批流。
2. 向量检索与 RAG 平台。
3. 知识图谱。
4. 规则表达式设计器。
5. 多角色协作审批工作流。
6. AI 自动生成知识的质量评分与批量审核。
