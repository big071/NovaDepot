# NovaDepot RESTful API 设计文档

## 1. 设计约定

## 1.1 基础约定
- 基础前缀：`/api/v1`
- 认证方式：`Authorization: Bearer <JWT>`
- 多租户：从 JWT 解析 `tenantId`，管理接口可额外支持 `X-Tenant-Id`
- 响应结构：`{ code, message, data, traceId, timestamp }`
- 分页参数：`pageNo`、`pageSize`、`sortBy`、`sortOrder`

## 1.2 状态流转接口约定
- 所有状态动作采用子资源动作路径：`POST /{resource}/{id}/actions/{action}`
- 幂等建议：写接口支持 `requestId`

---

## 2. 模块 API 设计

说明：表格列含义为  
接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注

## 2.1 auth
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 登录 | POST | `/api/v1/auth/login` | 获取 access/refresh token | `username,password,tenantCode` | `accessToken,refreshToken,expiresIn,userInfo` | 无 | 是 | 否 | MVP |
| 刷新令牌 | POST | `/api/v1/auth/refresh` | 刷新 access token | `refreshToken` | `accessToken,expiresIn` | 无 | 否 | 否 | MVP |
| 登出 | POST | `/api/v1/auth/logout` | 注销会话 | 无 | `success` | 已登录 | 是 | 否 | MVP |
| 当前用户信息 | GET | `/api/v1/auth/me` | 获取用户与权限概览 | 无 | `id,name,roles,permissions,tenantId` | 已登录 | 否 | 是 | MVP |

## 2.2 users
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 用户列表 | GET | `/api/v1/users` | 分页查询用户 | `keyword,status,pageNo,pageSize` | `list,total` | `USER_READ` | 否 | 是 | MVP |
| 创建用户 | POST | `/api/v1/users` | 新建账号 | `username,name,phone,email,roleIds` | `id` | `USER_CREATE` | 是 | 否 | MVP |
| 用户详情 | GET | `/api/v1/users/{id}` | 查询用户详情 | `id` | `baseInfo,roles,dataScopes` | `USER_READ` | 否 | 是 | MVP |
| 更新用户 | PUT | `/api/v1/users/{id}` | 修改用户信息 | `name,phone,email,status` | `success` | `USER_UPDATE` | 是 | 否 | MVP |
| 重置密码 | POST | `/api/v1/users/{id}/actions/reset-password` | 重置密码 | `newPassword` | `success` | `USER_RESET_PWD` | 是 | 否 | MVP |

## 2.3 roles
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 角色列表 | GET | `/api/v1/roles` | 查询角色 | `keyword,status` | `list` | `ROLE_READ` | 否 | 是 | MVP |
| 创建角色 | POST | `/api/v1/roles` | 新建角色 | `roleCode,roleName,dataScope` | `id` | `ROLE_CREATE` | 是 | 否 | MVP |
| 更新角色 | PUT | `/api/v1/roles/{id}` | 修改角色 | `roleName,dataScope,status` | `success` | `ROLE_UPDATE` | 是 | 否 | MVP |
| 分配权限 | POST | `/api/v1/roles/{id}/permissions` | 绑定权限 | `permissionIds[]` | `success` | `ROLE_GRANT` | 是 | 否 | MVP |

## 2.4 permissions
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 权限树 | GET | `/api/v1/permissions/tree` | 获取权限树 | 无 | `treeNodes` | `PERMISSION_READ` | 否 | 是 | MVP |
| 权限列表 | GET | `/api/v1/permissions` | 查询权限点 | `module,keyword` | `list` | `PERMISSION_READ` | 否 | 是 | MVP |

## 2.5 products
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 商品列表 | GET | `/api/v1/products` | 分页查询商品 | `keyword,categoryId,status` | `list,total` | `PRODUCT_READ` | 否 | 是 | MVP |
| 创建商品 | POST | `/api/v1/products` | 新建商品 | `productCode,name,categoryId,unitId,barcode` | `id` | `PRODUCT_CREATE` | 是 | 否 | MVP |
| 商品详情 | GET | `/api/v1/products/{id}` | 查询详情 | `id` | `baseInfo,stockSummary` | `PRODUCT_READ` | 否 | 是 | MVP |
| 更新商品 | PUT | `/api/v1/products/{id}` | 更新商品 | `name,spec,status` | `success` | `PRODUCT_UPDATE` | 是 | 否 | MVP |

## 2.6 categories
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 分类树 | GET | `/api/v1/categories/tree` | 获取分类树 | 无 | `nodes` | `CATEGORY_READ` | 否 | 是 | MVP |
| 新建分类 | POST | `/api/v1/categories` | 创建分类 | `parentId,code,name` | `id` | `CATEGORY_CREATE` | 是 | 否 | MVP |
| 更新分类 | PUT | `/api/v1/categories/{id}` | 更新分类 | `name,sortNo,status` | `success` | `CATEGORY_UPDATE` | 是 | 否 | MVP |

## 2.7 warehouses
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 仓库列表 | GET | `/api/v1/warehouses` | 查询仓库 | `keyword,status` | `list` | `WAREHOUSE_READ` | 否 | 是 | MVP |
| 创建仓库 | POST | `/api/v1/warehouses` | 新建仓库 | `code,name,type,address` | `id` | `WAREHOUSE_CREATE` | 是 | 否 | MVP |
| 仓库详情 | GET | `/api/v1/warehouses/{id}` | 仓库详情 | `id` | `baseInfo,stats` | `WAREHOUSE_READ` | 否 | 是 | MVP |
| 更新仓库 | PUT | `/api/v1/warehouses/{id}` | 更新仓库 | `name,managerId,status` | `success` | `WAREHOUSE_UPDATE` | 是 | 否 | MVP |

## 2.8 locations
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 库位列表 | GET | `/api/v1/locations` | 查询库位 | `warehouseId,keyword,status` | `list` | `LOCATION_READ` | 否 | 是 | MVP |
| 创建库位 | POST | `/api/v1/locations` | 创建库位 | `warehouseId,code,name,type` | `id` | `LOCATION_CREATE` | 是 | 否 | MVP |
| 更新库位 | PUT | `/api/v1/locations/{id}` | 修改库位 | `name,capacityQty,status` | `success` | `LOCATION_UPDATE` | 是 | 否 | MVP |

## 2.9 inventory（高价值）
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 库存查询 | GET | `/api/v1/inventory` | 查询实时库存（高价值） | `warehouseId,locationId,productId,keyword,pageNo,pageSize` | `list(availableQty,lockedQty,inTransitQty),total` | `INVENTORY_READ` | 否 | 是 | MVP |
| 库存详情 | GET | `/api/v1/inventory/{id}` | 查看库存记录 | `id` | `inventoryDetail` | `INVENTORY_READ` | 否 | 是 | MVP |
| 低库存预警 | GET | `/api/v1/inventory/alerts/low-stock` | 查询低库存预警（高价值） | `warehouseId,pageNo,pageSize` | `list(product,threshold,currentQty,alertLevel)` | `INVENTORY_ALERT_READ` | 否 | 是 | MVP |
| 库存调整创建 | POST | `/api/v1/inventory/adjustments` | 创建库存调整单 | `warehouseId,items[],reason,requestId` | `adjustmentNo,id` | `INVENTORY_ADJUST_CREATE` | 是 | 否 | MVP |

## 2.10 inventory-transactions（高价值）
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 库存流水查询 | GET | `/api/v1/inventory-transactions` | 查询库存流水（高价值） | `bizType,bizNo,warehouseId,productId,timeFrom,timeTo,pageNo,pageSize` | `list(changeQty,beforeQty,afterQty,occurredAt),total` | `INVENTORY_TXN_READ` | 否 | 条件缓存 | MVP |
| 流水详情 | GET | `/api/v1/inventory-transactions/{id}` | 流水详情 | `id` | `txnDetail` | `INVENTORY_TXN_READ` | 否 | 否 | MVP |

## 2.11 inbound-orders（高价值）
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 入库单列表 | GET | `/api/v1/inbound-orders` | 查询入库单 | `status,bizType,warehouseId,pageNo,pageSize` | `list,total` | `INBOUND_READ` | 否 | 是 | MVP |
| 创建入库单 | POST | `/api/v1/inbound-orders` | 入库创建（高价值） | `bizType,warehouseId,supplierId,items[],requestId` | `id,inboundNo,status` | `INBOUND_CREATE` | 是 | 否 | MVP |
| 入库单详情 | GET | `/api/v1/inbound-orders/{id}` | 详情查看 | `id` | `baseInfo,items,logs` | `INBOUND_READ` | 否 | 否 | MVP |
| 审核入库单 | POST | `/api/v1/inbound-orders/{id}/actions/approve` | 入库审核（高价值） | `approveRemark` | `status` | `INBOUND_APPROVE` | 是 | 否 | MVP |
| 入账确认 | POST | `/api/v1/inbound-orders/{id}/actions/post` | 入库入账（高价值） | `receiveItems[],requestId` | `status,inventoryEffects` | `INBOUND_POST` | 是 | 否 | MVP |
| 作废入库单 | POST | `/api/v1/inbound-orders/{id}/actions/cancel` | 作废 | `reason` | `status` | `INBOUND_CANCEL` | 是 | 否 | MVP |

## 2.12 outbound-orders（高价值）
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 出库单列表 | GET | `/api/v1/outbound-orders` | 查询出库单 | `status,bizType,warehouseId,pageNo,pageSize` | `list,total` | `OUTBOUND_READ` | 否 | 是 | MVP |
| 创建出库单 | POST | `/api/v1/outbound-orders` | 出库创建（高价值） | `bizType,warehouseId,customerId,items[],requestId` | `id,outboundNo,status` | `OUTBOUND_CREATE` | 是 | 否 | MVP |
| 出库单详情 | GET | `/api/v1/outbound-orders/{id}` | 详情 | `id` | `baseInfo,items,logs` | `OUTBOUND_READ` | 否 | 否 | MVP |
| 审核出库单 | POST | `/api/v1/outbound-orders/{id}/actions/approve` | 出库审核（高价值） | `remark` | `status` | `OUTBOUND_APPROVE` | 是 | 否 | MVP |
| 扣减出库 | POST | `/api/v1/outbound-orders/{id}/actions/ship` | 库存扣减出库（高价值） | `shipItems[],requestId` | `status,inventoryEffects` | `OUTBOUND_SHIP` | 是 | 否 | MVP |
| 拣货复核 | POST | `/api/v1/outbound-orders/{id}/actions/review` | 复核 | `reviewItems[]` | `status` | `OUTBOUND_REVIEW` | 是 | 否 | MVP |

## 2.13 transfer-orders（高价值）
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 调拨单列表 | GET | `/api/v1/transfer-orders` | 调拨单查询 | `status,fromWarehouseId,toWarehouseId` | `list,total` | `TRANSFER_READ` | 否 | 是 | MVP |
| 创建调拨单 | POST | `/api/v1/transfer-orders` | 调拨流程发起（高价值） | `fromWarehouseId,toWarehouseId,items[],requestId` | `id,transferNo,status` | `TRANSFER_CREATE` | 是 | 否 | MVP |
| 审核调拨单 | POST | `/api/v1/transfer-orders/{id}/actions/approve` | 审核 | `remark` | `status` | `TRANSFER_APPROVE` | 是 | 否 | MVP |
| 调出确认 | POST | `/api/v1/transfer-orders/{id}/actions/dispatch` | 调出扣减 | `dispatchItems[],requestId` | `status` | `TRANSFER_DISPATCH` | 是 | 否 | MVP |
| 调入确认 | POST | `/api/v1/transfer-orders/{id}/actions/receive` | 调入入账 | `receiveItems[],requestId` | `status` | `TRANSFER_RECEIVE` | 是 | 否 | MVP |

## 2.14 stocktake-orders（高价值）
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 盘点单列表 | GET | `/api/v1/stocktake-orders` | 盘点任务查询 | `status,warehouseId` | `list,total` | `STOCKTAKE_READ` | 否 | 是 | MVP |
| 创建盘点单 | POST | `/api/v1/stocktake-orders` | 盘点流程发起（高价值） | `warehouseId,scopeType,items?` | `id,stocktakeNo,status` | `STOCKTAKE_CREATE` | 是 | 否 | MVP |
| 盘点录入 | POST | `/api/v1/stocktake-orders/{id}/actions/count` | 录入盘点结果 | `countItems[]` | `status,diffSummary` | `STOCKTAKE_COUNT` | 是 | 否 | MVP |
| 差异审核 | POST | `/api/v1/stocktake-orders/{id}/actions/approve-diff` | 差异审核（高价值） | `remark` | `status` | `STOCKTAKE_APPROVE` | 是 | 否 | MVP |
| 生成调整 | POST | `/api/v1/stocktake-orders/{id}/actions/adjust` | 生成库存调整（高价值） | `requestId` | `status,adjustmentNo` | `STOCKTAKE_ADJUST` | 是 | 否 | MVP |

## 2.15 purchase-orders
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 采购单列表 | GET | `/api/v1/purchase-orders` | 查询采购单 | `status,supplierId` | `list,total` | `PURCHASE_READ` | 否 | 是 | MVP |
| 创建采购单 | POST | `/api/v1/purchase-orders` | 新建采购单 | `supplierId,warehouseId,items[]` | `id,purchaseNo,status` | `PURCHASE_CREATE` | 是 | 否 | MVP |
| 采购单详情 | GET | `/api/v1/purchase-orders/{id}` | 查看详情 | `id` | `baseInfo,items,inboundRefs` | `PURCHASE_READ` | 否 | 否 | MVP |
| 提交审核 | POST | `/api/v1/purchase-orders/{id}/actions/submit` | 提交审批 | 无 | `status` | `PURCHASE_SUBMIT` | 是 | 否 | MVP |

## 2.16 sales-orders
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 销售单列表 | GET | `/api/v1/sales-orders` | 查询销售单 | `status,customerId` | `list,total` | `SALES_READ` | 否 | 是 | MVP |
| 创建销售单 | POST | `/api/v1/sales-orders` | 新建销售单 | `customerId,warehouseId,items[]` | `id,salesNo,status` | `SALES_CREATE` | 是 | 否 | MVP |
| 销售单详情 | GET | `/api/v1/sales-orders/{id}` | 查看详情 | `id` | `baseInfo,items,outboundRefs` | `SALES_READ` | 否 | 否 | MVP |
| 提交审核 | POST | `/api/v1/sales-orders/{id}/actions/submit` | 提交审批 | 无 | `status` | `SALES_SUBMIT` | 是 | 否 | MVP |

## 2.17 suppliers
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 供应商列表 | GET | `/api/v1/suppliers` | 查询供应商 | `keyword,status` | `list,total` | `SUPPLIER_READ` | 否 | 是 | MVP |
| 创建供应商 | POST | `/api/v1/suppliers` | 新建供应商 | `code,name,contact,phone` | `id` | `SUPPLIER_CREATE` | 是 | 否 | MVP |
| 更新供应商 | PUT | `/api/v1/suppliers/{id}` | 更新供应商 | `name,contact,phone,status` | `success` | `SUPPLIER_UPDATE` | 是 | 否 | MVP |

## 2.18 customers
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 客户列表 | GET | `/api/v1/customers` | 查询客户 | `keyword,status` | `list,total` | `CUSTOMER_READ` | 否 | 是 | MVP |
| 创建客户 | POST | `/api/v1/customers` | 新建客户 | `code,name,contact,phone` | `id` | `CUSTOMER_CREATE` | 是 | 否 | MVP |
| 更新客户 | PUT | `/api/v1/customers/{id}` | 更新客户 | `name,contact,phone,status` | `success` | `CUSTOMER_UPDATE` | 是 | 否 | MVP |

## 2.19 reports（高价值）
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 仪表盘统计 | GET | `/api/v1/reports/dashboard` | 仪表盘统计（高价值） | `dateFrom,dateTo,warehouseId?` | `kpiCards,trend,alerts` | `REPORT_DASHBOARD_READ` | 否 | 是 | MVP |
| 库存报表 | GET | `/api/v1/reports/inventory` | 库存报表（高价值） | `warehouseId,groupBy,dateFrom,dateTo` | `summary,rows` | `REPORT_INVENTORY_READ` | 否 | 是 | MVP |
| 出入库报表 | GET | `/api/v1/reports/inbound-outbound` | 出入库统计 | `warehouseId,dateFrom,dateTo` | `inbound,outbound,net` | `REPORT_IO_READ` | 否 | 是 | MVP |
| 采购报表 | GET | `/api/v1/reports/purchase` | 采购统计 | `supplierId,dateFrom,dateTo` | `summary,rows` | `REPORT_PURCHASE_READ` | 否 | 是 | P1 |
| 销售报表 | GET | `/api/v1/reports/sales` | 销售统计 | `customerId,dateFrom,dateTo` | `summary,rows` | `REPORT_SALES_READ` | 否 | 是 | P1 |
| 报表导出任务 | POST | `/api/v1/reports/exports` | 创建导出任务 | `reportType,filters` | `jobId,status` | `REPORT_EXPORT` | 是 | 否 | P1 |

## 2.20 notifications
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 通知列表 | GET | `/api/v1/notifications` | 查询通知 | `readFlag,type,pageNo,pageSize` | `list,total` | `NOTIFY_READ` | 否 | 是 | MVP |
| 标记已读 | POST | `/api/v1/notifications/{id}/actions/read` | 单条已读 | 无 | `success` | `NOTIFY_READ` | 否 | 否 | MVP |
| 全部已读 | POST | `/api/v1/notifications/actions/read-all` | 全部已读 | 无 | `success` | `NOTIFY_READ` | 否 | 否 | MVP |
| 未读计数 | GET | `/api/v1/notifications/unread-count` | 获取未读数量 | 无 | `count` | `NOTIFY_READ` | 否 | 是（Redis） | MVP |

## 2.21 audit-logs
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 审计日志列表 | GET | `/api/v1/audit-logs` | 查询审计日志 | `module,action,operatorId,timeFrom,timeTo` | `list,total` | `AUDIT_READ` | 否 | 否 | MVP |
| 审计日志详情 | GET | `/api/v1/audit-logs/{id}` | 查询变更前后 | `id` | `beforeJson,afterJson,meta` | `AUDIT_READ` | 否 | 否 | MVP |

## 2.22 files
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 文件上传 | POST | `/api/v1/files/upload` | 上传文件 | `multipart file,bizType,bizNo` | `fileId,url,objectKey` | `FILE_UPLOAD` | 是 | 否 | MVP |
| 文件详情 | GET | `/api/v1/files/{id}` | 查询文件元数据 | `id` | `fileMeta` | `FILE_READ` | 否 | 否 | MVP |
| 文件下载签名 | GET | `/api/v1/files/{id}/download-url` | 获取下载地址 | `id` | `downloadUrl,expireAt` | `FILE_READ` | 否 | 否 | MVP |

## 2.23 ai（高价值）
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| AI 聊天 | POST | `/api/v1/ai/chat` | AI 聊天（高价值） | `scene,message,conversationId?,providerHint?,context` | `conversationId,reply,providerUsed,confidence,sourceRefs` | `AI_CHAT` | 是 | 否 | MVP 支持 mock/rule，P2 支持 paid |
| 会话列表 | GET | `/api/v1/ai/conversations` | 查询 AI 会话 | `scene,pageNo,pageSize` | `list,total` | `AI_READ` | 否 | 否 | MVP |
| 会话消息 | GET | `/api/v1/ai/conversations/{id}/messages` | 查询消息历史 | `id` | `messages[]` | `AI_READ` | 否 | 否 | MVP |
| Prompt 模板列表 | GET | `/api/v1/ai/prompts` | 模板管理 | `scene,enabled` | `list` | `AI_PROMPT_READ` | 否 | 是 | P1 |
| Prompt 模板维护 | POST/PUT | `/api/v1/ai/prompts` | 新建/修改模板 | `templateCode,scene,content,version` | `id` | `AI_PROMPT_WRITE` | 是 | 否 | P1 |
| Provider 切换 | POST | `/api/v1/ai/providers/actions/switch` | 切换 provider | `providerType,modelName` | `success,currentProvider` | `AI_PROVIDER_SWITCH` | 是 | 否 | P2 |

## 2.24 customer-service（高价值）
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 客服会话列表 | GET | `/api/v1/customer-service/sessions` | 会话查询（高价值） | `status,assignedUserId,keyword,pageNo,pageSize` | `list,total` | `CS_SESSION_READ` | 否 | 短缓存 | MVP |
| 创建会话 | POST | `/api/v1/customer-service/sessions` | 新建会话 | `channel,customerId,subject` | `sessionId,status` | `CS_SESSION_CREATE` | 是 | 否 | MVP |
| 会话详情 | GET | `/api/v1/customer-service/sessions/{id}` | 会话详情 | `id` | `session,customer,relatedOrders` | `CS_SESSION_READ` | 否 | 否 | MVP |
| 智能客服消息发送 | POST | `/api/v1/customer-service/sessions/{id}/messages` | 发送消息（高价值） | `content,msgType,sendByAi?` | `messageId,createdAt` | `CS_MESSAGE_SEND` | 是 | 否 | MVP |
| 会话消息列表 | GET | `/api/v1/customer-service/sessions/{id}/messages` | 拉取消息（高价值） | `pageNo,pageSize` | `list,total` | `CS_MESSAGE_READ` | 否 | 短缓存 | MVP |
| 人工转接 | POST | `/api/v1/customer-service/sessions/{id}/actions/transfer-human` | 人工转接（高价值） | `targetUserId,reason` | `status,assignedUser` | `CS_TRANSFER_HUMAN` | 是 | 否 | MVP |
| 工单创建 | POST | `/api/v1/customer-service/tickets` | 从会话建工单 | `sessionId,priority,content` | `ticketId,ticketNo,status` | `CS_TICKET_CREATE` | 是 | 否 | MVP |
| FAQ 检索 | GET | `/api/v1/customer-service/faq` | FAQ 检索 | `keyword,scene` | `list(question,answer)` | `CS_FAQ_READ` | 否 | 是 | MVP |

## 2.25 settings
| 接口名称 | 方法 | 路径 | 用途 | 请求参数 | 响应字段 | 权限要求 | 审计 | 缓存建议 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| 系统参数查询 | GET | `/api/v1/settings` | 获取参数 | `group?` | `kvPairs` | `SETTING_READ` | 否 | 是 | MVP |
| 系统参数更新 | PUT | `/api/v1/settings` | 更新参数 | `group,key,value` | `success` | `SETTING_WRITE` | 是 | 否 | MVP |
| 阈值配置 | PUT | `/api/v1/settings/thresholds` | 更新库存/预警阈值 | `lowStockRules[]` | `success` | `SETTING_WRITE` | 是 | 否 | MVP |

---

## 3. 高价值接口补充说明
| 场景 | 接口 |
|---|---|
| 库存查询 | `GET /api/v1/inventory` |
| 库存流水查询 | `GET /api/v1/inventory-transactions` |
| 入库创建/审核/入账 | `POST /api/v1/inbound-orders`、`POST /api/v1/inbound-orders/{id}/actions/approve`、`POST /api/v1/inbound-orders/{id}/actions/post` |
| 出库创建/审核/扣减 | `POST /api/v1/outbound-orders`、`POST /api/v1/outbound-orders/{id}/actions/approve`、`POST /api/v1/outbound-orders/{id}/actions/ship` |
| 调拨流程 | `POST /api/v1/transfer-orders`、`.../approve`、`.../dispatch`、`.../receive` |
| 盘点流程 | `POST /api/v1/stocktake-orders`、`.../count`、`.../approve-diff`、`.../adjust` |
| 低库存预警 | `GET /api/v1/inventory/alerts/low-stock` |
| 仪表盘统计 | `GET /api/v1/reports/dashboard` |
| 报表接口 | `/api/v1/reports/*` |
| AI 聊天 | `POST /api/v1/ai/chat`（mock/rule/paid 可配置） |
| 智能客服会话 | `GET/POST /api/v1/customer-service/sessions` |
| 智能客服消息 | `GET/POST /api/v1/customer-service/sessions/{id}/messages` |
| 人工转接 | `POST /api/v1/customer-service/sessions/{id}/actions/transfer-human` |

---

## 4. MVP 与后期扩展标记建议
- MVP 优先：认证、权限、主数据、库存、单据流程、基础报表、通知、审计、客服基础、AI mock/rule。
- 后期扩展：AI paid provider 切换、报表导出增强、消息渠道扩展、高级客服质检。

---

## 5. Spring Boot Controller 设计建议
- 每个模块一个 `*Controller`，路径统一资源风格。
- 动作接口使用 `actions` 子路径，便于状态机表达。
- 查询接口 GET，变更接口 POST/PUT，删除建议逻辑删除动作而非 DELETE。
- DTO 分层：`Create*Req`、`Update*Req`、`Query*Req`、`*Resp`。

---

## 6. Docker Compose 本地联调与网关路径建议

## 6.1 本地地址建议
| 服务 | 地址 |
|---|---|
| Frontend（Next.js） | `http://localhost:3000` |
| Backend（Spring Boot） | `http://localhost:8080` |
| API 网关前缀（Nginx 可选） | `http://localhost:8080/api/v1`（无网关时直接后端） |
| Swagger | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

## 6.2 前后端联调建议
- Next.js 统一配置：`NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1`
- 开发代理可选：前端 `/api/*` 反向代理到后端 `:8080/api/*`
- Docker 网络内服务名：
  - backend -> `mysql:3306`
  - backend -> `redis:6379`

## 6.3 分环境配置建议
- `application.yml`：通用配置（日志、Jackson、OpenAPI）
- `application-dev.yml`：本地直连（localhost）
- `application-docker.yml`：容器服务名连接（mysql、redis）

