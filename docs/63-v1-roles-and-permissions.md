# NovaDepot v1.0 角色与权限说明

## 1. 角色体系

NovaDepot v1.0 采用 **4 角色 RBAC（基于角色的访问控制）** 体系。

| 角色 | 代码 | 职责 | 典型岗位 |
|------|------|------|---------|
| 管理员 | `admin` | 系统管理、审核、规则配置、用户管理 | 仓库经理 / IT 管理员 |
| 仓储运营 | `warehouse` | 库存管理、入库/出库操作 | 仓管员 / 操作工 |
| 客服运营 | `cs` | 客服接待、工单处理、FAQ/SOP 维护 | 客服专员 |
| 观察员 | `observer` | 只读查看所有业务数据 | 审计 / 决策层 |

## 2. 权限码关系

```
┌──────────┐      ┌──────────────┐      ┌──────────────┐
│  角色    │ ─── │  角色-权限关联 │ ─── │   权限码      │
│  roles   │      │ role_perms   │      │ permissions  │
└──────────┘      └──────────────┘      └──────────────┘
```

角色 → 权限多对多，扩展角色只需关联对应权限码即可。

## 3. 核心权限码清单

### 3.1 仓储模块

| 权限码 | 含义 | admin | warehouse | cs | observer |
|--------|------|:-----:|:---------:|:--:|:--------:|
| `inventory:view` | 查看库存 | ✅ | ✅ | ❌ | ✅ |
| `inventory:adjust` | 调整库存 | ✅ | ✅ | ❌ | ❌ |
| `inbound:view` | 查看入库单 | ✅ | ✅ | ❌ | ✅ |
| `inbound:create` | 创建入库单 | ✅ | ✅ | ❌ | ❌ |
| `inbound:submit` | 提交入库单 | ✅ | ✅ | ❌ | ❌ |
| `inbound:approve` | 审核入库单 | ✅ | ❌ | ❌ | ❌ |
| `inbound:execute` | 执行入库 | ✅ | ✅ | ❌ | ❌ |
| `outbound:view` | 查看出库单 | ✅ | ✅ | ❌ | ✅ |
| `outbound:create` | 创建出库单 | ✅ | ✅ | ❌ | ❌ |
| `outbound:submit` | 提交出库单 | ✅ | ✅ | ❌ | ❌ |
| `outbound:approve` | 审核出库单 | ✅ | ❌ | ❌ | ❌ |
| `outbound:ship` | 发运出库 | ✅ | ✅ | ❌ | ❌ |

### 3.2 客服模块

| 权限码 | 含义 | admin | warehouse | cs | observer |
|--------|------|:-----:|:---------:|:--:|:--------:|
| `cs:view` | 查看客服工作台 | ✅ | ❌ | ✅ | ✅ |
| `cs:session:manage` | 管理会话 | ✅ | ❌ | ✅ | ❌ |
| `cs:ticket:create` | 创建工单 | ✅ | ❌ | ✅ | ❌ |
| `cs:ticket:manage` | 管理工单 | ✅ | ❌ | ✅ | ❌ |
| `cs:ticket:assign` | 指派工单 | ✅ | ❌ | ✅ | ❌ |

### 3.3 知识库模块

| 权限码 | 含义 | admin | warehouse | cs | observer |
|--------|------|:-----:|:---------:|:--:|:--------:|
| `faq:view` | 查看 FAQ | ✅ | ✅ | ✅ | ✅ |
| `faq:manage` | 编辑 FAQ | ✅ | ❌ | ✅ | ❌ |
| `sop:view` | 查看 SOP | ✅ | ✅ | ✅ | ✅ |
| `sop:manage` | 编辑 SOP | ✅ | ❌ | ✅ | ❌ |
| `rule:view` | 查看规则 | ✅ | ❌ | ✅ | ✅ |
| `rule:manage` | 修改规则 | ✅ | ❌ | ❌ | ❌ |

### 3.4 AI / Agent 模块

| 权限码 | 含义 | admin | warehouse | cs | observer |
|--------|------|:-----:|:---------:|:--:|:--------:|
| `ai:chat` | 使用 AI 助手 | ✅ | ✅ | ✅ | ✅ |
| `agent:view` | 查看 Agent 中心 | ✅ | ✅ | ✅ | ✅ |
| `agent:execute` | 执行 Agent 任务 | ✅ | ✅ | ✅ | ❌ |

### 3.5 审计与系统

| 权限码 | 含义 | admin | warehouse | cs | observer |
|--------|------|:-----:|:---------:|:--:|:--------:|
| `audit:view` | 查看审计中心 | ✅ | ❌ | ❌ | ❌ |
| `user:manage` | 管理用户 | ✅ | ❌ | ❌ | ❌ |
| `role:manage` | 管理角色 | ✅ | ❌ | ❌ | ❌ |
| `system:settings` | 系统设置 | ✅ | ❌ | ❌ | ❌ |

## 4. 权限实现机制

### 4.1 页面级权限（前端路由守卫）

```typescript
// frontend-vue/src/router/index.ts 中的路由 meta
{
  path: '/system/audit-center',
  component: () => import('@/pages/system/AuditCenter.vue'),
  meta: {
    requiredPermissions: ['audit:view']  // 仅 admin 可访问
  }
}
```

无权限用户访问时：
- 前端：路由守卫拦截，跳转至 403 页面
- 后端：API 返回 403 Forbidden

### 4.2 操作级权限（按钮/输入框）

前端组件中使用 `<PermissionGuard>` 或 `v-permission` 指令控制元素显示/禁用：

```vue
<template>
  <n-button v-permission="'rule:manage'" @click="saveRule">保存规则</n-button>
</template>
```

无权限时：
- 按钮 **渲染但置灰**（disabled），tooltip 提示"仅管理员可操作"
- 后端 API 仍然校验权限，防止绕过前端

### 4.3 后端方法级权限（AOP 注解）

```java
@RestController
@RequestMapping("/api/v1/knowledge/rules")
public class RuleConfigController {

    @PutMapping("/{id}")
    @RequirePermission("rule:manage")  // AOP 切面校验
    public ApiResponse<Void> updateRule(@PathVariable Long id, @RequestBody RuleConfigDTO dto) {
        // ...
    }
}
```

无权限时返回：
```json
{
  "code": 403,
  "message": "权限不足，需要权限码：rule:manage",
  "data": null
}
```

## 5. 越权处理统一规范

| 场景 | 前端表现 | 后端表现 |
|------|---------|---------|
| 非管理员访问审计中心 | 拦截跳转 403 页面 | HTTP 403 |
| 非管理员修改规则 | 按钮置灰 + tooltip 提示 | HTTP 403 + "仅管理员可修改业务规则" |
| 观察员尝试执行操作 | 按钮隐藏/置灰 | HTTP 403 |
| 未登录访问任何页面 | 跳转登录页 | HTTP 401 |

## 6. 演示账号

| 用户名 | 密码 | 角色 | 用途 |
|--------|------|------|------|
| `admin` | `admin123` | 管理员 | 审核单据、管理用户、修改规则、查看审计 |
| `warehouse01` | `pass123` | 仓储运营 | 创建入库/出库单、执行发运 |
| `cs01` | `pass123` | 客服运营 | 客服接待、工单处理、FAQ/SOP 编辑 |
| `observer01` | `pass123` | 观察员 | 只读浏览所有业务数据 |