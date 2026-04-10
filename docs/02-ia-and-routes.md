# NovaDepot 信息架构（IA）与页面树

## 1. 导航总图

### 1.1 顶层导航结构
| 顶层导航 | 说明 | 典型角色 |
|---|---|---|
| 工作台 | 首页概览、待办、预警、快捷入口 | 全角色 |
| 仓储中心 | 商品、仓库、库存、入出库、调拨、盘点 | 仓库经理/操作员 |
| 业务中心 | 采购、销售、供应商、客户 | 采购/销售/财务 |
| 智能中心 | AI 助手、智能客服工作台 | 管理/客服 |
| 数据中心 | 报表中心、导入导出任务 | 管理/财务/运营 |
| 协同中心 | 通知中心、审批待办（可并入工作台） | 全角色 |
| 系统设置 | 组织、用户、角色、参数、字典、审计 | 租户管理员/审计员 |

### 1.2 侧边栏菜单结构（建议）
| 一级菜单 | 二级菜单 |
|---|---|
| 工作台 | 仪表盘、我的待办 |
| 商品中心 | 商品列表、商品分类、SKU 规格、条码管理 |
| 仓库与库位 | 仓库列表、库位管理、库位地图（后期） |
| 库存管理 | 库存查询、库存流水、库存预警、库存调整 |
| 入库管理 | 入库单列表、创建入库单、入库任务 |
| 出库管理 | 出库单列表、创建出库单、拣货复核 |
| 调拨管理 | 调拨单列表、创建调拨单 |
| 盘点管理 | 盘点任务、差异处理、盘点报表 |
| 采购管理 | 采购单列表、创建采购单、到货登记 |
| 销售管理 | 销售单列表、创建销售单、退货管理 |
| 供应商管理 | 供应商列表、供应商等级（后期） |
| 客户管理 | 客户列表、客户分组（后期） |
| 报表中心 | 仓储报表、采购报表、销售报表、客服报表 |
| 通知中心 | 系统通知、业务提醒、消息订阅 |
| AI 助手 | 企业助手、仓库助手、提示词模板（管理） |
| 智能客服工作台 | 会话中心、工单中心、知识库、FAQ |
| 系统设置 | 用户与角色、权限配置、系统参数、审计日志、API Key/AI Provider |

---

## 2. 路由树（Next.js App Router 风格）

## 2.1 路由命名规范建议
- 使用业务域前缀：`/wms/*`、`/erp/*`、`/ai/*`、`/cs/*`、`/system/*`
- 列表页：`/module/resource`
- 创建页：`/module/resource/new`
- 详情页：`/module/resource/[id]`
- 编辑页：`/module/resource/[id]/edit`

## 2.2 路由总树
```text
app/
  (auth)/
    login/page.tsx                          -> /login
    forgot-password/page.tsx                -> /forgot-password
  (workspace)/
    page.tsx                                -> /
    dashboard/page.tsx                      -> /dashboard
    todo/page.tsx                           -> /todo

    wms/
      products/page.tsx                     -> /wms/products
      products/new/page.tsx                 -> /wms/products/new
      products/[id]/page.tsx                -> /wms/products/[id]
      products/[id]/edit/page.tsx           -> /wms/products/[id]/edit

      warehouses/page.tsx                   -> /wms/warehouses
      warehouses/new/page.tsx               -> /wms/warehouses/new
      warehouses/[id]/page.tsx              -> /wms/warehouses/[id]
      locations/page.tsx                    -> /wms/locations

      inventory/page.tsx                    -> /wms/inventory
      inventory/ledger/page.tsx             -> /wms/inventory/ledger
      inventory/alerts/page.tsx             -> /wms/inventory/alerts
      inventory/adjustments/page.tsx        -> /wms/inventory/adjustments

      inbound/page.tsx                      -> /wms/inbound
      inbound/new/page.tsx                  -> /wms/inbound/new
      inbound/[id]/page.tsx                 -> /wms/inbound/[id]
      inbound/tasks/page.tsx                -> /wms/inbound/tasks

      outbound/page.tsx                     -> /wms/outbound
      outbound/new/page.tsx                 -> /wms/outbound/new
      outbound/[id]/page.tsx                -> /wms/outbound/[id]
      outbound/picking/page.tsx             -> /wms/outbound/picking

      transfers/page.tsx                    -> /wms/transfers
      transfers/new/page.tsx                -> /wms/transfers/new
      transfers/[id]/page.tsx               -> /wms/transfers/[id]

      stocktakes/page.tsx                   -> /wms/stocktakes
      stocktakes/new/page.tsx               -> /wms/stocktakes/new
      stocktakes/[id]/page.tsx              -> /wms/stocktakes/[id]
      stocktakes/diffs/page.tsx             -> /wms/stocktakes/diffs

    erp/
      purchases/page.tsx                    -> /erp/purchases
      purchases/new/page.tsx                -> /erp/purchases/new
      purchases/[id]/page.tsx               -> /erp/purchases/[id]
      purchases/receiving/page.tsx          -> /erp/purchases/receiving

      sales/page.tsx                        -> /erp/sales
      sales/new/page.tsx                    -> /erp/sales/new
      sales/[id]/page.tsx                   -> /erp/sales/[id]
      sales/returns/page.tsx                -> /erp/sales/returns

      suppliers/page.tsx                    -> /erp/suppliers
      suppliers/new/page.tsx                -> /erp/suppliers/new
      suppliers/[id]/page.tsx               -> /erp/suppliers/[id]

      customers/page.tsx                    -> /erp/customers
      customers/new/page.tsx                -> /erp/customers/new
      customers/[id]/page.tsx               -> /erp/customers/[id]

    reports/
      page.tsx                              -> /reports
      wms/page.tsx                          -> /reports/wms
      purchases/page.tsx                    -> /reports/purchases
      sales/page.tsx                        -> /reports/sales
      service/page.tsx                      -> /reports/service

    notifications/
      page.tsx                              -> /notifications
      subscriptions/page.tsx                -> /notifications/subscriptions

    ai/
      enterprise/page.tsx                   -> /ai/enterprise
      warehouse/page.tsx                    -> /ai/warehouse
      templates/page.tsx                    -> /ai/templates

    cs/
      workspace/page.tsx                    -> /cs/workspace
      tickets/page.tsx                      -> /cs/tickets
      tickets/[id]/page.tsx                 -> /cs/tickets/[id]
      kb/page.tsx                           -> /cs/kb
      faq/page.tsx                          -> /cs/faq

    system/
      users/page.tsx                        -> /system/users
      roles/page.tsx                        -> /system/roles
      permissions/page.tsx                  -> /system/permissions
      settings/page.tsx                     -> /system/settings
      audit-logs/page.tsx                   -> /system/audit-logs
      ai-provider/page.tsx                  -> /system/ai-provider
```

---

## 3. 模块页面清单与职责说明

## 3.1 登录与账户
| 页面 | 路由 | 职责 | 对接后端 |
|---|---|---|---|
| 登录页 | `/login` | 账号登录、租户识别、JWT 获取 | 必须依赖 API |
| 忘记密码 | `/forgot-password` | 找回密码流程入口 | 必须依赖 API |
| 个人待办 | `/todo` | 我的审批、我的任务、快捷入口 | 依赖 API |

## 3.2 仪表盘
| 页面 | 路由 | 职责 | 对接后端 |
|---|---|---|---|
| 工作台首页 | `/` | 按角色展示入口、通知、最近操作 | 依赖 API |
| 仪表盘 | `/dashboard` | KPI、库存预警、业务趋势图 | 依赖 API |

## 3.3 商品中心
| 页面 | 路由 | 职责 |
|---|---|---|
| 商品列表 | `/wms/products` | 搜索、筛选、导入、状态管理 |
| 创建商品 | `/wms/products/new` | 创建 SPU/SKU、条码、单位 |
| 商品详情 | `/wms/products/[id]` | 基础信息、库存、关联单据 |
| 编辑商品 | `/wms/products/[id]/edit` | 维护属性与启停用 |

## 3.4 仓库与库位
| 页面 | 路由 | 职责 |
|---|---|---|
| 仓库列表 | `/wms/warehouses` | 仓库维护、权限绑定 |
| 新建仓库 | `/wms/warehouses/new` | 创建仓库与策略 |
| 仓库详情 | `/wms/warehouses/[id]` | 仓库指标、库位统计 |
| 库位管理 | `/wms/locations` | 库位增删改查、容量管理 |

## 3.5 库存管理
| 页面 | 路由 | 职责 |
|---|---|---|
| 库存查询 | `/wms/inventory` | 实时库存、可用/冻结/在途 |
| 库存流水 | `/wms/inventory/ledger` | 按单据/SKU/时间追溯 |
| 库存预警 | `/wms/inventory/alerts` | 安全库存阈值、预警处理 |
| 库存调整 | `/wms/inventory/adjustments` | 调整单创建与审批 |

## 3.6 入库管理
| 页面 | 路由 | 职责 |
|---|---|---|
| 入库列表 | `/wms/inbound` | 入库单筛选、状态流转 |
| 创建入库 | `/wms/inbound/new` | 采购入库/其他入库创建 |
| 入库详情 | `/wms/inbound/[id]` | 收货、上架、确认入库 |
| 入库任务 | `/wms/inbound/tasks` | 待处理任务分配与执行 |

## 3.7 出库管理
| 页面 | 路由 | 职责 |
|---|---|---|
| 出库列表 | `/wms/outbound` | 出库单管理 |
| 创建出库 | `/wms/outbound/new` | 销售出库/领用出库创建 |
| 出库详情 | `/wms/outbound/[id]` | 拣货、复核、发运确认 |
| 拣货复核台 | `/wms/outbound/picking` | 高效率作业页面 |

## 3.8 调拨管理
| 页面 | 路由 | 职责 |
|---|---|---|
| 调拨列表 | `/wms/transfers` | 调拨单检索和追踪 |
| 创建调拨 | `/wms/transfers/new` | 仓间/库位间调拨创建 |
| 调拨详情 | `/wms/transfers/[id]` | 调出调入确认、差异处理 |

## 3.9 盘点管理
| 页面 | 路由 | 职责 |
|---|---|---|
| 盘点任务 | `/wms/stocktakes` | 任务发起与执行 |
| 创建盘点 | `/wms/stocktakes/new` | 盘点范围与规则配置 |
| 盘点详情 | `/wms/stocktakes/[id]` | 明细录入、差异复核 |
| 差异处理 | `/wms/stocktakes/diffs` | 差异审批、调整单生成 |

## 3.10 采购管理
| 页面 | 路由 | 职责 |
|---|---|---|
| 采购列表 | `/erp/purchases` | 采购单生命周期管理 |
| 创建采购 | `/erp/purchases/new` | 采购单创建 |
| 采购详情 | `/erp/purchases/[id]` | 明细、进度、关联入库 |
| 到货登记 | `/erp/purchases/receiving` | 收货登记、入库触发 |

## 3.11 销售管理
| 页面 | 路由 | 职责 |
|---|---|---|
| 销售列表 | `/erp/sales` | 销售单管理 |
| 创建销售 | `/erp/sales/new` | 销售单创建、库存检查 |
| 销售详情 | `/erp/sales/[id]` | 发货跟踪、出库关联 |
| 退货管理 | `/erp/sales/returns` | 退货登记与入库联动 |

## 3.12 供应商管理
| 页面 | 路由 | 职责 |
|---|---|---|
| 供应商列表 | `/erp/suppliers` | 主数据与合作状态 |
| 新建供应商 | `/erp/suppliers/new` | 创建与资质录入 |
| 供应商详情 | `/erp/suppliers/[id]` | 交易统计与联系人 |

## 3.13 客户管理
| 页面 | 路由 | 职责 |
|---|---|---|
| 客户列表 | `/erp/customers` | 客户主数据与状态 |
| 新建客户 | `/erp/customers/new` | 客户录入 |
| 客户详情 | `/erp/customers/[id]` | 订单、回款、服务记录 |

## 3.14 报表中心
| 页面 | 路由 | 职责 |
|---|---|---|
| 报表首页 | `/reports` | 报表入口与收藏 |
| 仓储报表 | `/reports/wms` | 库存、周转、预警 |
| 采购报表 | `/reports/purchases` | 采购金额、到货率 |
| 销售报表 | `/reports/sales` | 销售额、毛利趋势 |
| 客服报表 | `/reports/service` | SLA、首响、关闭率 |

## 3.15 通知中心
| 页面 | 路由 | 职责 |
|---|---|---|
| 通知列表 | `/notifications` | 系统通知与业务提醒 |
| 订阅设置 | `/notifications/subscriptions` | 接收规则与渠道订阅 |

## 3.16 系统设置
| 页面 | 路由 | 职责 |
|---|---|---|
| 用户管理 | `/system/users` | 用户、组织、启停 |
| 角色管理 | `/system/roles` | 角色与权限分配 |
| 权限配置 | `/system/permissions` | 资源与操作权限 |
| 参数设置 | `/system/settings` | 业务参数、阈值 |
| 审计日志 | `/system/audit-logs` | 全局审计检索 |
| AI Provider | `/system/ai-provider` | Mock/Rule/Paid 切换配置 |

## 3.17 AI 助手
| 页面 | 路由 | 职责 |
|---|---|---|
| 企业助手 | `/ai/enterprise` | 经营问答、报表解释 |
| 仓库助手 | `/ai/warehouse` | 异常预警解释、作业建议 |
| 模板管理 | `/ai/templates` | 提示词与回复模板维护 |

## 3.18 智能客服工作台
| 页面 | 路由 | 职责 |
|---|---|---|
| 客服工作台 | `/cs/workspace` | 会话处理、建议回复、快捷工单 |
| 工单列表 | `/cs/tickets` | 工单检索、SLA 跟踪 |
| 工单详情 | `/cs/tickets/[id]` | 处理记录、升级、关闭 |
| 知识库 | `/cs/kb` | 知识文章维护 |
| FAQ 管理 | `/cs/faq` | FAQ 条目维护 |

---

## 4. 页面跳转关系（关键链路）
| 来源页面 | 跳转页面 | 触发动作 |
|---|---|---|
| `/dashboard` | `/wms/inventory/alerts` | 点击库存预警卡片 |
| `/erp/purchases/[id]` | `/wms/inbound/new` | 采购到货触发入库 |
| `/erp/sales/[id]` | `/wms/outbound/new` | 销售发货触发出库 |
| `/wms/stocktakes/[id]` | `/wms/inventory/adjustments` | 差异确认生成调整单 |
| `/cs/workspace` | `/cs/tickets/new`（可弹窗） | 未命中 FAQ 创建工单 |
| `/notifications` | 业务详情页 | 点击通知跳转业务单据 |
| `/ai/warehouse` | `/wms/inventory/ledger` | 查看 AI 引用明细数据 |

---

## 5. 弹窗 / 抽屉 / 独立详情页建议
| 交互类型 | 适合页面 |
|---|---|
| 弹窗（轻操作） | 快速新建分类、启停用确认、审批确认、通知订阅设置 |
| 抽屉（中等复杂） | 商品快速编辑、客户/供应商快速查看、工单快捷处理 |
| 独立详情页（重业务） | 入库详情、出库详情、调拨详情、盘点详情、采购详情、销售详情、工单详情 |

---

## 6. 高交互页面建议
| 页面 | 高交互点 |
|---|---|
| `/wms/outbound/picking` | 扫码输入、键盘快捷操作、批量复核 |
| `/wms/inbound/[id]` | 收货明细分批录入、异常即时提示 |
| `/wms/stocktakes/[id]` | 盘点实时差异反馈、批量处理 |
| `/cs/workspace` | 会话实时刷新、建议回复插入、多面板协同 |
| `/dashboard` | 卡片联动筛选、图表钻取 |

---

## 7. 图表与动态可视化页面建议
| 页面 | 可视化建议 |
|---|---|
| `/dashboard` | KPI 卡片、趋势折线、异常分布 |
| `/reports/wms` | 库存结构、周转天数、库龄分布 |
| `/reports/purchases` | 到货率、供应商贡献度 |
| `/reports/sales` | 销售趋势、品类占比 |
| `/reports/service` | SLA 达成率、响应时长分位图 |
| `/wms/warehouses/[id]` | 仓库利用率、库位热力（后期） |

---

## 8. 移动端兼容优先页面
| 优先级 | 页面 | 原因 |
|---|---|---|
| 高 | `/wms/outbound/picking` | 仓内现场作业高频 |
| 高 | `/wms/inbound/tasks` | 移动收货与上架 |
| 高 | `/wms/stocktakes/[id]` | 现场盘点录入 |
| 高 | `/cs/workspace` | 客服值班移动处理 |
| 中 | `/notifications` | 即时提醒处理 |
| 中 | `/todo` | 审批待办处理 |

---

## 9. 页面优先级（P0/P1/P2）
| 优先级 | 页面范围 |
|---|---|
| P0 | 登录、仪表盘、商品、仓库库位、库存查询/流水、入库、出库、调拨、盘点、采购、销售、供应商、客户、通知、客服工作台（基础） |
| P1 | 报表中心全套、库存预警页、审批待办、AI 助手（企业+仓库）、系统设置（用户/角色/权限/参数） |
| P2 | 模板管理、库位热力图、订阅高级策略、客服高级分析、多租户平台运营页 |

---

## 10. 与 Spring Boot API 对接与 Docker 访问路径规划

## 10.1 本地 Docker 访问建议
| 服务 | 本地地址（建议） | 说明 |
|---|---|---|
| Web（Next.js） | `http://localhost:3000` | 主入口 |
| API（Spring Boot） | `http://localhost:8080/api` | REST API |
| Swagger | `http://localhost:8080/swagger-ui/index.html` | 接口调试 |
| MySQL | `localhost:3306` | 容器映射（开发使用） |
| Redis | `localhost:6379` | 容器映射（开发使用） |

## 10.2 页面可访问依赖矩阵
| 页面类型 | Docker 本地可独立访问 | 是否依赖后端服务 |
|---|---|---|
| 登录页 UI、空态页、错误页 | 是 | 否（提交登录时依赖） |
| 列表页骨架与布局 | 是 | 否（加载数据依赖） |
| 业务列表/详情真实数据 | 否 | 是 |
| 报表图表真实数据 | 否 | 是 |
| 客服会话与工单实时处理 | 否 | 是 |
| 系统设置（权限/审计） | 否 | 是 |
| AI 助手真实问答 | 否 | 是（Mock/Rule Provider 也走后端） |

## 10.3 API 对接约束
- 页面仅调用 BFF/API 层，不直接连数据库。
- 状态流转接口需幂等（建议携带 `requestId`）。
- 列表查询统一支持：分页、排序、筛选、关键词。
- 详情接口统一返回：基础信息、明细、日志、可执行动作。

