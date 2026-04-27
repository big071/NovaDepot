# NovaDepot 数据来源对齐与 Mock 兜底说明（2026-04-11）

## 1. 目标
- 补齐核心页面缺少真实数据的问题。
- 优先接真实接口；后端暂未提供真实业务数据能力时，采用清晰 mock 兜底并显式标识。

## 2. 本轮覆盖页面
- 仪表盘
- 商品
- 库存
- 入库
- 出库
- AI 助手
- 客服工作台

## 3. 数据来源策略
1. 后端已有真实接口：前端直接接通，不保留硬编码假数据。
2. 后端接口存在但为占位样例：页面显式展示 `Mock 数据源` 标识，避免误导。
3. 数据加载失败：统一给出明确错误提示。

## 4. 本轮重点
1. 仪表盘图表改为真实接口聚合结果（取消前端硬编码趋势）。
2. 入库/出库创建表单改为真实基础数据下拉选择（仓库/商品/库位）。
3. 客服工作台显式标记当前为 Mock 数据源（后端占位服务）。

## 5. 验收标准
1. 仪表盘趋势图与风险图来自真实 API 数据聚合。
2. 入库/出库表单不再依赖默认 ID 填写。
3. 客服页面有清晰 mock 标识。
4. Docker 构建运行通过，核心接口回归正常。

## 6. 页面数据来源对齐结果
| 页面 | 数据来源 | 策略 |
|---|---|---|
| 仪表盘 | `GET /api/v1/reports/dashboard` + `GET /api/v1/inbound-orders` + `GET /api/v1/outbound-orders` + `GET /api/v1/inventory` | 全部真实接口；趋势图由真实单据创建时间聚合 |
| 商品 | `GET /api/v1/products` `GET /api/v1/products/{id}` `POST /api/v1/products` | 真实接口 |
| 库存 | `GET /api/v1/inventory` `GET /api/v1/inventory/alerts/low-stock` `GET /api/v1/inventory/transactions` | 真实接口 |
| 入库 | `GET /api/v1/inbound-orders` `POST /api/v1/inbound-orders` + 基础数据 `GET /api/v1/warehouses/products/locations` | 真实接口；创建表单改为真实下拉 |
| 出库 | `GET /api/v1/outbound-orders` `POST /api/v1/outbound-orders` + 基础数据 `GET /api/v1/warehouses/products/locations` | 真实接口；创建表单改为真实下拉 |
| AI 助手 | `GET /api/v1/ai/conversations` `GET /api/v1/ai/conversations/{id}/messages` `POST /api/v1/ai/chat` | 真实接口（Provider 可为 rule/mock） |
| 客服工作台 | `GET/POST /api/v1/customer-service/*` | 后端当前为占位样例数据；前端显式标记 `Mock 数据源` |

## 7. 本轮实现说明
1. 仪表盘移除前端硬编码趋势数据，改为近 7 天真实单据聚合。
2. 仪表盘风险图改为真实库存列表聚合（低库存/健康库存）。
3. 入库页面创建弹窗改为仓库、商品、库位真实下拉选项。
4. 出库页面创建弹窗改为仓库、商品、库位真实下拉选项。
5. 客服工作台增加 `Mock 数据源` 标签与提示文案，避免误判为生产真实数据。

## 8. 验证结果
- `docker compose up --build -d`：通过。
- 核心接口回归：`reports/dashboard`、`products`、`inventory`、`inbound-orders`、`outbound-orders`、`ai/conversations`、`customer-service/sessions` 均成功返回。
