# 52. 测试全绿热修复（2026-04-22）

## 背景
当前回归中出现以下阻塞：
1. WMS 入库/出库页面模板闭合错误导致前端编译失败。
2. `release-checklist.ps1` 健康检查依赖匿名 `actuator`，与现有安全策略冲突。
3. `data-quality-check.ps1` 与 `release-checklist.ps1` 的 SQL 执行采用 `sh -lc` 嵌套引号，复杂 SQL 易失败。
4. 部分 E2E 断言与页面最新交互文案不一致，出现误报失败。

## 本次热修复范围
1. 修复入库/出库页面模板结构，恢复前端可编译。
2. 调整运维脚本健康探测方式，改为先登录再校验受保护接口。
3. 调整脚本 SQL 执行方式，去掉易碎的 shell 二次转义。
4. 对齐 E2E 断言与页面真实稳定信号，减少误报。

## 验收标准
1. `npm run test:e2e` 通过。
2. `./scripts/ops/data-quality-check.ps1` 通过。
3. `./scripts/ops/release-checklist.ps1` 通过。
4. 不引入新业务模块，不破坏 Docker 本地运行链路。
