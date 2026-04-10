<template>
  <section class="space-y-4">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">库存管理</h1>
        <p class="text-sm text-text-secondary">展示实时库存与低库存预警。</p>
      </div>
      <n-button :loading="loading" @click="loadData">刷新</n-button>
    </header>

    <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
      <article class="rounded-2xl border border-border bg-surface p-4 shadow-card">
        <h3 class="text-sm font-semibold">库存列表</h3>
        <n-data-table class="mt-3" :columns="inventoryColumns" :data="inventoryRows" :loading="loading" :bordered="false" />
      </article>

      <article class="rounded-2xl border border-border bg-surface p-4 shadow-card">
        <h3 class="text-sm font-semibold">低库存预警</h3>
        <n-data-table class="mt-3" :columns="alertColumns" :data="alertRows" :loading="loading" :bordered="false" />
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { NButton, NDataTable, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { wmsApi, type InventoryItem } from "@/services/wms";

const message = useMessage();
const loading = ref(false);
const inventoryRows = ref<InventoryItem[]>([]);
const alertRows = ref<InventoryItem[]>([]);

const inventoryColumns: DataTableColumns<InventoryItem> = [
  { title: "ID", key: "id", width: 80 },
  { title: "仓库ID", key: "warehouseId" },
  { title: "库位ID", key: "locationId" },
  { title: "商品ID", key: "productId" },
  { title: "可用库存", key: "availableQty" },
  { title: "锁定库存", key: "lockedQty" },
  { title: "在途库存", key: "inTransitQty" }
];

const alertColumns: DataTableColumns<InventoryItem> = [
  { title: "商品ID", key: "productId" },
  { title: "仓库ID", key: "warehouseId" },
  { title: "库位ID", key: "locationId" },
  { title: "可用库存", key: "availableQty" }
];

async function loadData() {
  loading.value = true;
  try {
    const [inventory, alerts] = await Promise.all([wmsApi.listInventory(), wmsApi.listLowStockAlerts()]);
    inventoryRows.value = inventory;
    alertRows.value = alerts;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "库存数据加载失败");
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>
