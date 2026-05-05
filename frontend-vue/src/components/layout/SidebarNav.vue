<template>
  <aside class="sticky top-0 hidden h-screen w-64 shrink-0 overflow-y-auto border-r border-border bg-surface/85 px-3 py-4 backdrop-blur lg:block">
    <div class="mb-6 rounded-xl border border-border/80 bg-bg/60 px-3 py-3">
      <p class="text-xs uppercase tracking-[0.2em] text-text-secondary">NovaDepot</p>
      <h1 class="mt-1 text-lg font-semibold tracking-tight">{{ authStore.roleNameZh }}导航</h1>
    </div>
    <nav class="space-y-1.5">
      <RouterLink
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="group flex items-center justify-between rounded-xl border border-transparent px-3 py-2.5 text-sm transition-all"
        :class="
          route.path === item.path
            ? 'border-primary/30 bg-primary/10 text-primary shadow-card'
            : 'text-text-secondary hover:-translate-y-0.5 hover:border-border hover:bg-bg hover:text-text-primary'
        "
      >
        <span>{{ item.label }}</span>
        <span class="h-2 w-2 rounded-full transition" :class="route.path === item.path ? 'bg-primary' : 'bg-transparent group-hover:bg-info/60'" />
      </RouterLink>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const route = useRoute();
const authStore = useAuthStore();

const roleMenuMap: Record<string, Array<{ label: string; path: string; permission: string }>> = {
  admin: [
    { label: "经营总览", path: "/dashboard", permission: "REPORT_DASHBOARD_READ" },
    { label: "商品管理", path: "/wms/products", permission: "PRODUCT_READ" },
    { label: "仓库管理", path: "/wms/warehouses", permission: "WAREHOUSE_READ" },
    { label: "库位管理", path: "/wms/locations", permission: "LOCATION_READ" },
    { label: "库存管理", path: "/wms/inventory", permission: "INVENTORY_READ" },
    { label: "入库管理", path: "/wms/inbound", permission: "INBOUND_READ" },
    { label: "出库管理", path: "/wms/outbound", permission: "OUTBOUND_READ" },
    { label: "往来单位", path: "/erp/partners", permission: "PARTNER_READ" },
    { label: "采购管理", path: "/erp/purchases", permission: "PURCHASE_READ" },
    { label: "销售管理", path: "/erp/sales", permission: "SALES_READ" },
    { label: "AI 助手", path: "/ai/enterprise", permission: "AI_CHAT" },
    { label: "Agent 中心", path: "/agent/center", permission: "AGENT_TASK_READ" },
    { label: "知识维护", path: "/cs/knowledge", permission: "KNOWLEDGE_READ" },
    { label: "用户管理", path: "/system/users", permission: "USER_READ" },
    { label: "审计中心", path: "/system/audit-center", permission: "AUDIT_READ" }
  ],
  warehouse_ops: [
    { label: "仓储工作台", path: "/dashboard", permission: "REPORT_DASHBOARD_READ" },
    { label: "库存管理", path: "/wms/inventory", permission: "INVENTORY_READ" },
    { label: "入库管理", path: "/wms/inbound", permission: "INBOUND_READ" },
    { label: "出库管理", path: "/wms/outbound", permission: "OUTBOUND_READ" },
    { label: "往来单位", path: "/erp/partners", permission: "PARTNER_READ" },
    { label: "采购管理", path: "/erp/purchases", permission: "PURCHASE_READ" },
    { label: "销售单只读", path: "/erp/sales", permission: "SALES_READ" },
    { label: "商品管理", path: "/wms/products", permission: "PRODUCT_READ" },
    { label: "仓库管理", path: "/wms/warehouses", permission: "WAREHOUSE_READ" },
    { label: "库位管理", path: "/wms/locations", permission: "LOCATION_READ" },
    { label: "仓储 SOP", path: "/cs/knowledge", permission: "KNOWLEDGE_READ" },
    { label: "Agent 中心", path: "/agent/center", permission: "AGENT_TASK_READ" }
  ],
  cs_ops: [
    { label: "客服工作台", path: "/cs/workspace", permission: "CS_SESSION_READ" },
    { label: "销售管理", path: "/erp/sales", permission: "SALES_READ" },
    { label: "采购单只读", path: "/erp/purchases", permission: "PURCHASE_READ" },
    { label: "往来单位", path: "/erp/partners", permission: "PARTNER_READ" },
    { label: "知识草稿", path: "/cs/knowledge", permission: "KNOWLEDGE_READ" },
    { label: "AI 助手", path: "/ai/enterprise", permission: "AI_CHAT" },
    { label: "Agent 中心", path: "/agent/center", permission: "AGENT_TASK_READ" },
    { label: "经营总览", path: "/dashboard", permission: "REPORT_DASHBOARD_READ" }
  ],
  observer: [
    { label: "只读总览", path: "/dashboard", permission: "REPORT_DASHBOARD_READ" },
    { label: "库存建议", path: "/wms/inventory", permission: "INVENTORY_READ" },
    { label: "往来单位", path: "/erp/partners", permission: "PARTNER_READ" },
    { label: "采购单", path: "/erp/purchases", permission: "PURCHASE_READ" },
    { label: "销售单", path: "/erp/sales", permission: "SALES_READ" },
    { label: "知识只读", path: "/cs/knowledge", permission: "KNOWLEDGE_READ" },
    { label: "Agent 历史", path: "/agent/center", permission: "AGENT_TASK_READ" }
  ]
};

const navItems = computed(() => {
  const role = authStore.roleKey;
  const raw = roleMenuMap[role] ?? roleMenuMap.observer;
  return raw.filter((item) => authStore.hasPermission(item.permission));
});
</script>
