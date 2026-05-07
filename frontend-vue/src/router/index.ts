import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";

interface RouteMetaPermission {
  public?: boolean;
  requiredPermission?: string;
  requiredAnyPermissions?: string[];
  requiredRoles?: Array<"admin" | "warehouse_ops" | "cs_ops" | "observer">;
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/dashboard" },
    { path: "/login", component: () => import("@/pages/LoginPage.vue"), meta: { public: true } },
    { path: "/access-denied", component: () => import("@/pages/AccessDeniedPage.vue"), meta: { public: true } },
    {
      path: "/",
      component: () => import("@/layouts/WorkspaceLayout.vue"),
      children: [
        { path: "dashboard", component: () => import("@/pages/DashboardPage.vue"), meta: { requiredPermission: "REPORT_DASHBOARD_READ" } },
        { path: "wms/products", component: () => import("@/pages/wms/ProductsPage.vue"), meta: { requiredPermission: "PRODUCT_READ" } },
        { path: "wms/warehouses", component: () => import("@/pages/wms/WarehousesPage.vue"), meta: { requiredPermission: "WAREHOUSE_READ" } },
        { path: "wms/locations", component: () => import("@/pages/wms/LocationsPage.vue"), meta: { requiredPermission: "LOCATION_READ" } },
        { path: "wms/inventory", component: () => import("@/pages/wms/InventoryPage.vue"), meta: { requiredPermission: "INVENTORY_READ" } },
        { path: "wms/stock-take", component: () => import("@/pages/wms/StocktakePage.vue"), meta: { requiredPermission: "STOCKTAKE_READ" } },
        { path: "wms/inbound", component: () => import("@/pages/wms/InboundPage.vue"), meta: { requiredPermission: "INBOUND_READ" } },
        { path: "wms/outbound", component: () => import("@/pages/wms/OutboundPage.vue"), meta: { requiredPermission: "OUTBOUND_READ" } },
        { path: "erp/partners", component: () => import("@/pages/erp/PartnerPage.vue"), meta: { requiredPermission: "PARTNER_READ" } },
        { path: "erp/purchases", component: () => import("@/pages/erp/PurchasePage.vue"), meta: { requiredPermission: "PURCHASE_READ" } },
        { path: "erp/sales", component: () => import("@/pages/erp/SalesPage.vue"), meta: { requiredPermission: "SALES_READ" } },
        { path: "erp/finance", component: () => import("@/pages/erp/FinancePage.vue"), meta: { requiredAnyPermissions: ["FINANCE_PAYABLE_READ", "FINANCE_RECEIVABLE_READ"] } },
        { path: "ai/enterprise", component: () => import("@/pages/ai/AiAssistantPage.vue"), meta: { requiredPermission: "AI_CHAT" } },
        { path: "ai/usage-logs", component: () => import("@/pages/ai/AiUsageLogPage.vue"), meta: { requiredPermission: "AI_USAGE_LOG_VIEW" } },
        { path: "agent/center", component: () => import("@/pages/agent/AgentCenterPage.vue"), meta: { requiredPermission: "AGENT_TASK_READ" } },
        { path: "cs/workspace", component: () => import("@/pages/cs/CustomerServicePage.vue"), meta: { requiredPermission: "CS_SESSION_READ" } },
        { path: "cs/knowledge", component: () => import("@/pages/cs/KnowledgePage.vue"), meta: { requiredPermission: "KNOWLEDGE_READ" } },
        { path: "system/users", component: () => import("@/pages/system/UsersPage.vue"), meta: { requiredPermission: "USER_READ", requiredRoles: ["admin"] } },
        { path: "system/backups", component: () => import("@/pages/system/BackupsPage.vue"), meta: { requiredPermission: "BACKUP_READ", requiredRoles: ["admin"] } },
        { path: "system/audit-center", component: () => import("@/pages/system/AuditCenterPage.vue"), meta: { requiredPermission: "AUDIT_READ", requiredRoles: ["admin"] } }
      ]
    }
  ]
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  const meta = (to.meta ?? {}) as RouteMetaPermission;
  if (meta.public) return true;
  if (!authStore.isLoggedIn) {
    return "/login";
  }
  const profile = await authStore.ensureProfile();
  if (!profile) {
    return "/login?reason=expired";
  }

  if (meta.requiredPermission && !authStore.hasPermission(meta.requiredPermission)) {
    const preferredPath = resolvePreferredPath(authStore);
    if (to.path === "/dashboard" && preferredPath !== "/dashboard") {
      return preferredPath;
    }
    return {
      path: "/access-denied",
      query: {
        from: to.path,
        permission: meta.requiredPermission
      }
    };
  }
  if (meta.requiredAnyPermissions && meta.requiredAnyPermissions.length > 0 && !meta.requiredAnyPermissions.some((permission) => authStore.hasPermission(permission))) {
    return {
      path: "/access-denied",
      query: {
        from: to.path,
        permission: meta.requiredAnyPermissions.join(",")
      }
    };
  }
  if (meta.requiredRoles && meta.requiredRoles.length > 0 && !meta.requiredRoles.includes(authStore.roleKey)) {
    return {
      path: "/access-denied",
      query: {
        from: to.path,
        permission: meta.requiredPermission || "ROLE_LIMIT"
      }
    };
  }
  return true;
});

function resolvePreferredPath(authStore: ReturnType<typeof useAuthStore>) {
  if (authStore.hasPermission("REPORT_DASHBOARD_READ")) return "/dashboard";
  if (authStore.hasPermission("INBOUND_READ")) return "/wms/inbound";
  if (authStore.hasPermission("OUTBOUND_READ")) return "/wms/outbound";
  if (authStore.hasPermission("STOCKTAKE_READ")) return "/wms/stock-take";
  if (authStore.hasPermission("PURCHASE_READ")) return "/erp/purchases";
  if (authStore.hasPermission("CS_SESSION_READ")) return "/cs/workspace";
  if (authStore.hasPermission("AI_CHAT")) return "/ai/enterprise";
  if (authStore.hasPermission("AGENT_TASK_READ")) return "/agent/center";
  return "/access-denied";
}

export default router;
