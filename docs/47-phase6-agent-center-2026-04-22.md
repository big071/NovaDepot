# NovaDepot 商用版本 Phase 6：Agent 化升级（2026-04-22）

## 1. 阶段目标
在不破坏现有单租户可运行基线的前提下，将 NovaDepot 从“智能仓储系统”升级为“带任务型 Agent 的智能仓储平台”，并保证任务执行可追踪、可审计、可复盘。

## 2. 本轮范围
1. 新增 Agent Center（最小可用）：
   1. 任务列表
   2. 任务执行入口
   3. 执行过程展示
   4. 执行结果展示
   5. 历史记录入口
2. 新增三类高价值任务型 Agent：
   1. 补货建议 Agent
   2. 异常巡检 Agent
   3. 运营日报 Agent
3. 统一执行链路：
   1. 接收任务目标
   2. 读取相关数据
   3. 分析过程
   4. 结果输出
   5. 错误反馈
4. 约束：
   1. 不强依赖收费 API
   2. 优先 RuleProvider + 规则编排 + 真实查询
   3. PaidLLMProvider 仅预留增强入口
   4. 所有 Agent 行为可追踪、可审计

## 3. 设计原则
1. 先“任务编排”后“LLM增强”：本轮优先可解释规则执行链路。
2. 一切结果可追溯：每个任务都要有执行步骤、输入摘要、输出摘要、耗时、状态。
3. 与现有模块解耦：不改动入出库/客服主流程，只新增 Agent 中心与查询编排。
4. 权限最小化：通过独立权限点控制任务读取与执行。

## 4. 数据模型（最小可用）
新增表：`agent_task_runs`
1. 基础字段：`id/tenant_id/task_code/task_name/status`
2. 请求信息：`target_json`
3. 执行过程：`steps_json`（数组，记录每步名称、阶段、状态、摘要、耗时）
4. 执行结果：`result_json`
5. 失败信息：`error_message`
6. 元数据：`started_at/finished_at/created_at/created_by/updated_at/updated_by/deleted`

状态建议：
1. `RUNNING`
2. `SUCCESS`
3. `FAILED`

## 5. 任务定义

### 5.1 补货建议 Agent（`REPLENISH_SUGGESTION`）
步骤：
1. 接收目标（仓库范围、建议条数）
2. 读取库存（`inventory`）
3. 读取低库存（阈值）
4. 读取近期出库趋势（`outbound_orders` + `inventory_transactions`）
5. 生成补货建议（SKU、当前库存、建议补货量、理由）

### 5.2 异常巡检 Agent（`ANOMALY_PATROL`）
步骤：
1. 接收目标（巡检窗口）
2. 负库存检查
3. 低库存检查
4. 状态异常检查（单据状态不一致/异常状态）
5. 未完成单据检查（超时草稿、长期未完成）
6. 汇总异常结果

### 5.3 运营日报 Agent（`DAILY_OPERATIONS_REPORT`）
步骤：
1. 接收目标（日期）
2. 汇总今日入库
3. 汇总今日出库
4. 汇总低库存
5. 汇总工单与异常
6. 生成日报输出（结构化 JSON + 可读摘要）

## 6. 接口设计（最小可用）
1. `GET /api/v1/agent/tasks`
   1. 返回可执行任务列表（taskCode/taskName/description）
2. `POST /api/v1/agent/tasks/{taskCode}/execute`
   1. 发起任务执行，返回 runId 与执行结果
3. `GET /api/v1/agent/runs`
   1. 分页查询历史执行记录
4. `GET /api/v1/agent/runs/{id}`
   1. 获取单次执行详情（步骤 + 结果 + 错误）

## 7. 权限与审计
1. 新增权限点：
   1. `AGENT_TASK_READ`（任务与历史查看）
   2. `AGENT_TASK_EXECUTE`（任务执行）
2. 审计策略：
   1. 任务执行开始记录：`AGENT/TASK_EXECUTE_START`
   2. 任务执行完成记录：`AGENT/TASK_EXECUTE_SUCCESS`
   3. 任务执行失败记录：`AGENT/TASK_EXECUTE_FAILED`
3. 审计资源类型：`AGENT_TASK_RUN`

## 8. 前端交互（Agent Center）
1. 左侧导航新增“Agent Center”。
2. 页面区域：
   1. 任务列表（卡片/表格）
   2. 执行参数输入（最小可用）
   3. 执行过程步骤时间线
   4. 执行结果面板（摘要 + JSON）
   5. 历史记录列表与详情抽屉

## 9. 验收标准
1. 三个任务均能在真实样本数据上执行并产出结果。
2. 每个任务执行详情包含多步过程且可感知耗时与状态。
3. 失败场景可返回明确错误并在历史记录中可见。
4. 审计中心可查询到 Agent 任务执行日志。
5. Docker 本地环境可运行，且不破坏现有业务主链路。

## 10. 已知边界与后续增强
1. 本轮为同步执行模型（接口内完成），后续可扩展异步队列执行。
2. PaidLLMProvider 仅作为结果润色增强入口，默认关闭。
3. 暂不做复杂任务编排 DAG；后续可扩展子任务依赖与重试策略。
4. 暂不做跨租户调度，保持单租户基线与租户字段一致。
