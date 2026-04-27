# NovaDepot 出入库链路与 FAQ MySQL 化修复说明（2026-04-11）

## 1. 当前问题清单
1. 入库创建时库位无法正常选择。
2. 入库过账时报“单据不存在”。
3. 出入库缺详情查看（希望点击后在页面内显示明细列表，不用弹窗）。
4. FAQ 仍为硬编码 mock，客服页仍带 mock 标记。

## 2. 根因判断
1. 主因是 Long ID 精度问题：
   - 后端返回的雪花 ID 超出前端 Number 安全范围。
   - 前端携带失真后的 ID 进行查询/动作，导致库位筛选为空、过账查不到单据。
2. 出入库后端无“明细列表”接口，前端无法按单据展开查看行项目。
3. FAQ 查询未接 `faq_knowledge` 表，仍是代码内静态数据。

## 3. 本轮执行范围
1. 修复 ID 精度：后端 Long 输出统一按字符串序列化，保证前端拿到精确 ID。
2. 新增出入库明细接口并在页面内展示（点击详情后显示列表区，不用弹窗）。
3. FAQ 迁移为 MySQL 数据源，并补齐初始化种子。
4. 去掉客服页面的 mock 提示（FAQ 也接 DB 后不再需要）。

## 4. 本轮修改文件（计划）
- `backend/src/main/java/com/novadepot/backend/common/api/BaseEntity.java`
- `backend/src/main/java/com/novadepot/backend/modules/inboundorders/*`
- `backend/src/main/java/com/novadepot/backend/modules/outboundorders/*`
- `backend/src/main/java/com/novadepot/backend/modules/customerservice/CustomerServiceService.java`
- `backend/src/main/java/com/novadepot/backend/repository/FAQKnowledgeMapper.java`
- `backend/deploy/mysql/init/99-seed-mvp.sql`
- `frontend-vue/src/services/wms.ts`
- `frontend-vue/src/pages/wms/InboundPage.vue`
- `frontend-vue/src/pages/wms/OutboundPage.vue`
- `frontend-vue/src/pages/cs/CustomerServicePage.vue`

## 5. 验收标准
1. 入库创建可正常选择库位。
2. 入库过账不再出现“单据不存在”。
3. 出入库列表支持“详情”并在页面内显示单据行项目。
4. FAQ 数据来自 MySQL，客服页面不再显示 mock 标记。
5. Docker 构建通过，主链路无回归。
