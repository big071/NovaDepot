# NovaDepot Sprint 1 实施说明（2026-04-26）

## 1. 本轮目标
本轮只落地《NovaDepot 未来 3 个 Sprint 落地规划》中的 `Sprint 1：智能体验收口`，目标是让 `AI 助手 / Agent Center / 客服工作台` 从“能演示”提升到“真实可帮忙”，并保持本地 Docker、免费 Provider、角色权限链路继续可运行。

## 2. 当前智能体验问题清单
1. `AI 助手` 对任务型问题识别已有基础，但对开放式业务问法容易离题，回答不够像真实业务助手。
2. `AI -> Agent` 已能打通，但任务结果默认仍偏技术结构，业务用户需要自己理解字段。
3. `Agent Center` 虽已有任务、步骤、结果，但默认输出不够“先看结论、再看明细、最后看技术详情”。
4. `智能客服` 已有 FAQ 命中、候选回复和自动回复骨架，但“客户提问 -> AI 建议 -> 人工确认/接管”的页面闭环还不够清楚。
5. `AI / Agent / 客服建议` 虽有局部记录，但复盘入口和记录口径还不够统一。

## 3. 本轮严格执行范围
1. 强化 AI 助手的任务识别与开放式业务回答兜底。
2. 让任务型请求自动形成 Agent 任务，并返回业务化卡片与表格结果。
3. 让 Agent Center 默认展示业务化摘要、指标、建议动作和表格明细，而不是要求用户先看 JSON。
4. 让客服页支持“客户提问 -> AI 建议 -> 人工确认/接管”的最小闭环。
5. 让客服 AI 回复至少参考 FAQ、分类、优先级、候选回复或 SOP 之一。
6. 保持 RuleProvider / MockProvider 为默认可运行方案，DeepSeek 仅保留可选接入位。

## 4. 本轮不做什么
1. 不接入付费大模型作为前提条件。
2. 不把 DeepSeek 做成当前主链路强依赖。
3. 不做多轮复杂对话编排平台。
4. 不实现 Sprint 2 的单据/工单完整流程增强。
5. 不实现 Sprint 3 的 FAQ / SOP 后台化治理。
6. 不新增独立业务中心页。
7. 不改技术栈，不做无关重构。

## 5. 计划涉及页面
1. `/ai/enterprise`
2. `/agent/center`
3. `/cs/workspace`
4. `/dashboard`
5. `/wms/inventory`

## 6. 计划涉及后端模块
1. `modules.ai`
2. `modules.agent`
3. `modules.customerservice`
4. `modules.auditlogs`
5. `modules.inventory`
6. `modules.reports`

## 7. 计划涉及数据表
1. `ai_conversations`
2. `ai_messages`
3. `agent_task_runs`
4. `customer_service_sessions`
5. `customer_service_messages`
6. `customer_service_tickets`
7. `faq_knowledge`
8. `inventory`
9. `inventory_transactions`

## 8. 页面可见验收标准
1. AI 助手输入“优先补货顺序是什么”“今天最需要处理什么”“给我一个物流催发 SOP”时，能返回相关结论，不离题。
2. 任务型请求会自动形成 Agent 任务，并在 AI 助手或 Agent Center 中默认展示业务化卡片和表格。
3. Agent Center 不再默认让用户先看大段 JSON，原始 JSON 仅作为折叠技术详情。
4. 客服页能完成“客户提问 -> AI 建议 -> 人工确认/接管”的最小闭环。
5. 客服 AI 回复至少参考 FAQ、分类、优先级、候选回复或 SOP 中的一项。
6. AI / Agent / 客服建议相关记录可复盘。
7. 角色权限不回退，观察员不新增写操作入口。
8. Docker 本地运行不受影响。

## 9. 风险与回滚
### 9.1 风险
1. AI 规则过宽导致任务误判。
2. 自动回复过于机械，影响客服体验。
3. 业务化卡片过于简化，丢失上下文。

### 9.2 回滚方式
1. 可关闭 `AI -> Agent` 自动路由，退回普通 AI 聊天链路。
2. 可关闭客服自动回复，仅保留 AI 建议模式。
3. 可保留原始 JSON 作为兜底详情，不阻断任务结果查看。

## 10. 本轮发现但不实现的后续待办
### 10.1 应进入 Sprint 2 的内容
1. 工单的责任流转、处理历史、人工接管历史时间线。
2. 单据与工单的备注、审核意见、驳回原因统一展示。
3. 审计中心与工单/单据详情联动回查。

### 10.2 应进入 Sprint 3 的内容
1. FAQ 后台维护能力进一步增强。
2. SOP 独立知识化、版本化、可启停。
3. 规则配置从代码中继续外置，形成可维护知识资产。
