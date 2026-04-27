# NovaDepot Phase 6.1 交互与文本完整性修复（2026-04-22）

## 1. 问题清单
1. 入库/出库“详情”仍为页内展开，不符合弹窗交互预期。
2. 左侧导航在滚动时会移动，不利于长页面操作。
3. Agent 历史点击“详情”报“执行记录不存在”。
4. 多处页面与样本文本出现乱码（含 `????`、mojibake）。
5. 样本中存在英文编码（如 `WH-CD-RT`、`SKU-STR-060`），不利于业务演示。

## 2. 本轮范围
1. 将入库/出库详情改为弹窗展示。
2. 左侧导航改为滚动时固定。
3. 修复 Agent 运行记录详情查询（前后端 ID 采用字符串，避免 Long 精度丢失）。
4. 修复关键页面乱码（入库、出库、工作台提示文案、Agent 关键报错文案）。
5. 新增可重复执行的中文文本修复 SQL：
   - 修复关键主数据中文显示
   - 将仓库/商品等关键编码改为中文业务语义
   - 纳入重置商用基线脚本，确保 Docker 下重置后可追溯

## 3. 修改文件（计划）
1. `docs/48-phase6-1-ui-and-text-integrity-fix-2026-04-22.md`
2. `frontend-vue/src/pages/wms/InboundPage.vue`
3. `frontend-vue/src/pages/wms/OutboundPage.vue`
4. `frontend-vue/src/components/layout/SidebarNav.vue`
5. `frontend-vue/src/layouts/WorkspaceLayout.vue`
6. `frontend-vue/src/services/agent.ts`
7. `frontend-vue/src/pages/agent/AgentCenterPage.vue`
8. `backend/src/main/java/com/novadepot/backend/modules/agent/AgentCenterService.java`
9. `backend/deploy/mysql/init/102-data-repair-zh-semantic.sql`（新增）
10. `scripts/ops/reset-commercial-baseline.ps1`

## 4. 验收标准
1. 入库/出库“详情”点击后弹出模态框，关闭后列表状态不丢失。
2. 页面下滑时左侧导航保持固定可点击。
3. Agent 历史任意记录点击详情可打开，且不再出现“执行记录不存在”。
4. 关键页面不再出现乱码字符（`????`、`�`、mojibake）。
5. 执行 `reset-commercial-baseline.ps1` 后：
   - 仓库编码、商品编码等关键演示字段为中文语义编码
   - 商品/仓库/库位名称为可读中文
6. 改动兼容 Docker 本地运行。
