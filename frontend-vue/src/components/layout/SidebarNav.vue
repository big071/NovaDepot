<template>
  <aside class="hidden w-64 shrink-0 border-r border-border bg-surface/85 px-3 py-4 backdrop-blur lg:block">
    <div class="mb-6 rounded-xl border border-border/80 bg-bg/60 px-3 py-3">
      <p class="text-xs uppercase tracking-[0.2em] text-text-secondary">NovaDepot</p>
      <h1 class="mt-1 text-lg font-semibold tracking-tight">Vue 3 Console</h1>
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
import { useRoute } from "vue-router";

const route = useRoute();

const navItems = [
  { label: "仪表盘", path: "/dashboard" },
  { label: "商品管理", path: "/wms/products" },
  { label: "仓库管理", path: "/wms/warehouses" },
  { label: "库位管理", path: "/wms/locations" },
  { label: "库存管理", path: "/wms/inventory" },
  { label: "入库管理", path: "/wms/inbound" },
  { label: "出库管理", path: "/wms/outbound" },
  { label: "AI 助手", path: "/ai/enterprise" },
  { label: "智能客服", path: "/cs/workspace" }
];
</script>
