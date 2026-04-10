# NovaDepot 项目总览（Java 路线版）

## 1. 项目愿景
NovaDepot 定位为“智能仓储管理系统（WMS）+ 轻量 ERP + 智能助手 + 智能客服”一体化平台，服务中小商家、仓库与企业内部管理，并可平滑升级为商用 SaaS。

## 2. 本轮架构结论
- 核心业务：WMS 为主，ERP 为辅，AI 与客服作为效率增强层
- 架构风格：模块化单体优先（Modular Monolith），后续按域拆分微服务
- 开发策略：文档先行、分阶段交付、主链路优先
- 部署策略：本地 Docker Compose 一键启动，兼容桌面环境部署

## 3. 固定技术路线（必须遵循）
- 前端：Next.js + TypeScript + Tailwind CSS + shadcn/ui
- 动效：Framer Motion
- 图表：Recharts（主）/ ECharts（复杂可视化）
- 后端：Java 17+ + Spring Boot 3 + Maven
- ORM：MyBatis-Plus
- 数据库：MySQL 8
- 缓存与队列：Redis
- 鉴权：Spring Security + JWT + RBAC
- API 文档：SpringDoc OpenAPI / Swagger
- 部署：Docker + Docker Compose
- 配置：`application.yml` + `application-dev.yml` + `application-docker.yml`

## 4. AI 关键约束（前免费、后付费）
- 当前阶段禁止接入付费模型 API
- 先实现 Mock AI、规则引擎、模板回复、伪智能分析
- 从第一天起落地 `AIProvider` 抽象层，业务只依赖统一接口
- 商用阶段仅通过配置切换到付费 Provider，不改业务调用代码

## 5. 里程碑摘要
- M1（MVP）：WMS 主链路 + 权限 + 库存流水 + Mock/Rule AI + 基础客服
- M2（可用版）：采购/销售/财务简化闭环 + 报表 + 通知 + 导入导出
- M3（商用版）：多租户 + 计费 + 付费 AI Provider + 平台化运营

## 6. 文档索引
- 详细方案：`docs/01-prd-architecture.md`
