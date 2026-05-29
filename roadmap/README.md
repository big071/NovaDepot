# NovaDepot 路线图

> 从 v1.0 单仓库 WMS → v1.1 进销存闭环 → v1.2 DeepSeek 智能核心 → v1.2.1 AI 体验修复 → v1.3 质量加固 → v2.0 多租户 SaaS 商业化

---

## 总览

```text
v1.0                 v1.1                    v1.2                    v1.2.1
已发布               已发布                  已发布                  Hotfix 已合并 main
────────────────────────────────────────────────────────────────────────────────
WMS 基础操作          + 采购管理              + DeepSeek 真实接入       + AI 结构化回答
入库/出库/库存         + 销售管理              + 流式输出 + 打字机       + 工具结果融合
产品/仓库/分类         + 往来单位              + Function Calling        + DeepSeek 失败显性化
RBAC 权限             + 应收应付台账            + 多轮对话上下文          + 不再伪装降级
AI 规则引擎            + 库存盘点               + Agent 主动巡检          + Prompt 输出优化
客服工单              + CSV 批量导入           + 报表中心 + ECharts      + AI 配置状态展示
Agent 任务            + 打印模板               + 站内通知中心
审计日志              + 数据备份               + 前端体验优化
                      + 共用组件底座            + 审计日志清理
                      + tenant_id 预埋

v1.3                                      v2.0
READY FOR RELEASE                         PLANNED
────────────────────────────────────────────────────────────────────────────────
代码质量 + 稳定性加固                      SaaS 商业化
Service 拆分解耦                           多租户架构
前端大组件拆分                              多仓库管理
TypeScript 类型安全                         注册/入驻流程
异常处理标准化                              套餐分级计费
日志规范统一                                支付确认
单元测试补全                                管理后台
重复代码消除                                API 限流
Docker 镜像优化                             新手引导
CI 稳定性加固                               演示环境
E2E 扩展
```

---

## 版本状态

| 版本                                                | 状态              | 周期     | 定位                                                  |
| --------------------------------------------------- | ----------------- | -------- | ----------------------------------------------------- |
| v1.0                                                | RELEASED          | 已完成   | WMS + 客服 + AI 规则引擎基础版                        |
| [v1.1](./01-v1.1-procurement-sales-closure.md)      | RELEASED          | 已完成   | 进销存闭合 + 仓库运营刚需                             |
| [v1.2](./02-v1.2-deepseek-intelligence-core.md)     | RELEASED          | 已完成   | DeepSeek 智能核心 + 运营工具                          |
| v1.2.1                                              | MERGED / HOTFIX   | Hotfix   | AI 体验修复：结构化回答、工具结果融合、失败不伪装降级 |
| [v1.3](./04-v1.3-code-quality-and-stability.md)     | READY FOR RELEASE | 4-6 周   | 代码质量 + 稳定性加固（零新功能）                     |
| [v2.0](./03-v2.0-platform-and-commercialization.md) | PLANNED           | 10-14 周 | 多租户 SaaS + 商业化                                  |

> v1.2.1 已合并到 `main`。当前仓库仍未创建 `v1.2.1` tag；如需正式发布 tag，必须单独确认并执行发布流程。不要移动 `v1.2` tag。

---

## 当前阶段

当前阶段为：

```text
v1.3 Code Quality & Stability
```

v1.3 不是功能扩展版本。

v1.3 的核心目标是：

1. 降低代码复杂度
2. 拆分过大的 Service 和 Vue 组件
3. 补齐测试地基
4. 提升类型安全
5. 修复性能问题
6. 加固安全边界
7. 稳定 Docker 和 CI
8. 保持 v1.1 / v1.2 / v1.2.1 行为不回退

---

## 路线阶段说明

### v1.0 — First Light

状态：已发布

定位：NovaDepot 的第一版可演示系统。

核心能力：

- 商品、仓库、库位
- 库存管理
- 入库 / 出库
- RBAC 权限
- 审计日志
- AI 规则助手
- 客服工单基础能力
- Docker Compose 本地运行

---

### v1.1 — Procurement and Sales Closure

状态：已发布

定位：从 WMS 升级到轻量进销存闭环。

核心能力：

- 往来单位
- 采购单
- 销售单
- 采购转入库
- 销售转出库
- 应收应付
- 付款 / 收款
- 库存盘点
- CSV 导入
- 打印模板
- 数据备份
- tenant_id 预埋

v1.1 形成了：

```text
采购 → 入库 → 库存 → 出库 → 销售
```

的最小业务闭环。

---

### v1.2 — DeepSeek Intelligence Core

状态：已发布

定位：把 AI 从规则模板升级为智能核心。

核心能力：

- DeepSeek 真实接入
- Provider 配置
- API Key 脱敏
- AI 用量日志
- 流式输出
- 打字机效果
- 停止生成
- 最近 20 轮上下文
- 会话归档
- Function Calling
- 10 个只读工具函数
- 工具调用过程可视化
- 工具调用审计
- Agent 巡检
- 通知中心
- 报表中心
- 审计日志清理

v1.2 不做：

- RAG
- 向量数据库
- 知识图谱
- 多模态
- 多租户正式启用
- 计费

---

### v1.2.1 — AI Experience Hotfix

状态：已合并 main，tag 待单独发布授权

定位：修复 v1.2 发布后的 AI 体验问题。

核心修复：

- AI 回复从一整坨 Markdown 文本改为结构化业务卡片
- DeepSeek System Prompt 强化
- 工具查询结果融合到最终回答
- 工具名不再裸露给普通用户
- DeepSeek 失败时不再伪装成 RuleProvider 回答
- `AI_FALLBACK_ENABLED=false` 时失败显性化
- AI 配置页展示 Provider / model / fallback / tools / prompt 状态
- 继续禁止提交真实 API Key

---

### v1.3 — Code Quality & Stability

状态：READY FOR RELEASE

定位：质量加固阶段，零新业务功能。

目标：

- 拆分 God Service
- 拆分前端大组件
- 补充后端单元测试
- 补充前端组件测试
- 提升 TypeScript 类型安全
- 标准化异常处理
- 标准化日志
- 清理重复代码
- 优化 CSV 导入和分页
- 安全加固
- Docker 和 CI 稳定性优化

推荐 Sprint：

| Sprint | 名称                                       | 目标                               |
| ------ | ------------------------------------------ | ---------------------------------- |
| Q1     | Test Baseline and Code Quality Scan        | 扫描现状、补测试地基，不改业务逻辑 |
| Q2     | AiService Decomposition                    | 拆分 AI God Service，保持行为不变  |
| Q3     | AiProvider Type Safety                     | 强类型 DTO 替代 Map 返回           |
| Q4     | CSV Import and Pagination Performance      | 修复 N+1、长列表分页               |
| Q5     | Security Hardening                         | JWT、限流、请求大小、密钥安全      |
| Q6     | Frontend Component and Type Safety Cleanup | 拆大组件、消除 any、补组件测试     |

v1.3 严格禁止：

- 新增业务模块
- v2.0 多租户正式启用
- 计费
- RAG
- 向量数据库
- 多模态
- 自定义报表设计器
- BI 大屏
- 大规模目录迁移
- 改动已稳定主业务链路

---

### v2.0 — Platform and Commercialization

状态：PLANNED

定位：多租户 SaaS + 商业化。

候选能力：

- 多租户正式启用
- TenantInterceptor
- 注册 / 入驻
- 多仓库管理
- 组织 / 员工 / 权限分级
- 套餐分级
- 计费
- 支付确认
- 管理后台
- API 限流
- 新手引导
- 演示环境

进入 v2.0 的前置条件：

1. v1.3 质量加固完成
2. 核心 Service 已拆分
3. 后端测试地基完成
4. AI 模块可维护
5. 安全配置已加固
6. Docker / CI 稳定
7. v1.1 / v1.2 / v1.2.1 回归稳定

---

## 关键设计决策

### 为什么商业化放在 v2.0 而非 v1.1

1. v1.0 只有 WMS，不足以直接商业化。
2. v1.1 补齐进销存闭环后，业务价值更完整。
3. v1.2 接入 DeepSeek 后，AI 才成为产品差异化锚点。
4. v1.3 先加固质量，避免在脆弱代码上做 SaaS。
5. v2.0 再做多租户和商业化，风险更可控。

---

### 为什么 DeepSeek 而非其他 LLM

1. 中文理解较好，适合仓储、客服、进销存场景。
2. OpenAI-compatible API，Function Calling 迁移成本低。
3. 成本较低，适合中小企业场景。
4. 国内部署和合规路径更清晰。
5. 后续可评估私有化部署。

---

### 为什么 v1.2 暂不做 RAG / 向量数据库

当前 NovaDepot 的核心数据主要是结构化业务数据：

- 库存
- 单据
- 工单
- 商品
- 往来单位
- 报表
- 审计

这些更适合通过 Function Calling 查询，而不是一开始就上向量数据库。

现阶段：

```text
Function Calling + MySQL 结构化查询 + FAQ/SOP 表
```

已经覆盖大多数业务问答场景。

RAG / 向量数据库可作为 v1.4+ 或 v2.1+ 的增强方向。

---

## v1.3 执行原则

v1.3 必须遵守：

1. 先 Plan，后 Act。
2. 一次只处理一个质量主题。
3. 不做大功能。
4. 不做跨模块大规模重构。
5. 不改 API 契约。
6. 不改生产数据库 Schema。
7. 不改环境变量名。
8. 不改权限边界。
9. 不破坏 v1.1 / v1.2 / v1.2.1 已发布能力。
10. 每轮必须跑完整验证。

推荐第一步：

```text
v1.3 Sprint Q1: Test Baseline and Code Quality Scan
```

Q1 只做扫描和测试地基，不做业务代码重构。

---

## v2.1+ 展望

| 能力       | 说明                               |
| ---------- | ---------------------------------- |
| 在线支付   | 微信 / 支付宝扫码支付              |
| 跨仓库调拨 | 仓库间库存转移                     |
| 开放 API   | RESTful API + API Key              |
| Webhook    | 库存预警 / 单据变更推送            |
| 移动端 App | 仓库拣货 / 盘点 / 扫码             |
| 数据大屏   | 实时仓库运营看板                   |
| 国际化     | 英文版，面向东南亚 / 中东市场      |
| 私有化部署 | DeepSeek 私有化实例 + 离线部署方案 |
| RAG 增强   | FAQ / SOP / 文档知识库语义检索     |
| 多模态 OCR | 单据、面单、商品图片识别           |

---

## 当前默认下一步

```text
v1.3 Sprint Q1: Test Baseline and Code Quality Scan
```

目标：

- 扫描现有代码质量问题
- 统计测试覆盖现状
- 建立 v1.3 重构安全网
- 不修改业务逻辑
- 输出 Q2 AiService 拆分计划
