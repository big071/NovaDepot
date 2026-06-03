<template>
  <section class="space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Inventory</p>
          <h1 class="nd-page-title">库存管理</h1>
          <p class="nd-page-subtitle">库存、低库存预警、CSV 导入与流水历史。</p>
        </div>
        <input ref="importInput" class="hidden" type="file" accept=".csv,text/csv" @change="onImportFile" />
        <n-button v-if="authStore.hasPermission('INVENTORY_EXPORT')" class="nd-soft-focus" :loading="exporting" @click="downloadInventoryCsv">CSV导出</n-button>
        <n-button v-if="authStore.hasPermission('INVENTORY_TEMPLATE_EXPORT')" class="nd-soft-focus" @click="downloadTemplate">CSV模板</n-button>
        <n-button v-if="authStore.hasPermission('INVENTORY_IMPORT')" class="nd-soft-focus" @click="importInput?.click()">CSV导入</n-button>
        <n-button class="nd-soft-focus" :loading="loading" @click="loadData">刷新</n-button>
      </div>
      <div class="nd-hero-meta">
        <span class="nd-pill">库存记录：{{ displayInventoryRows.length }}</span>
        <span class="nd-pill">预警项：{{ alertRows.length }}</span>
        <span class="nd-pill">流水：{{ txnRows.length }}</span>
      </div>
    </header>
    <section class="nd-toolbar">
      <div class="nd-toolbar-group">
        <article class="nd-metric-chip">
          <p class="nd-metric-label">智能入口</p>
          <p class="nd-metric-value">补货与风险分析</p>
        </article>
      </div>
      <div class="flex items-center gap-2">
        <n-button class="nd-soft-focus" size="small" :loading="smartLoading === 'LOW_STOCK_ANALYSIS'" @click="runSmartTask('LOW_STOCK_ANALYSIS')">
          低库存分析
        </n-button>
        <n-button class="nd-soft-focus" size="small" :loading="smartLoading === 'REPLENISH_SUGGESTION'" @click="runSmartTask('REPLENISH_SUGGESTION')">
          生成补货建议
        </n-button>
      </div>
    </section>
    <n-alert class="nd-state-alert" type="info" :show-icon="false">
      先看“低库存预警”，再决定“入库补货”或“调整发运优先级”。库存列表、预警与流水会同步刷新。
    </n-alert>
    <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">
      <div class="flex items-center justify-between gap-2">
        <span>{{ errorText }}</span>
        <n-button text type="primary" @click="loadData">重试</n-button>
      </div>
    </n-alert>
    <n-alert v-else-if="lastSuccessText" class="nd-state-alert" type="success" :show-icon="false">{{ lastSuccessText }}</n-alert>

    <n-alert v-if="dashboardHint" class="nd-state-alert" type="info" :show-icon="false">
      <div class="flex items-center justify-between gap-2">
        <span>{{ dashboardHint }}</span>
        <n-button text type="primary" @click="clearDashboardFilter">清除筛选</n-button>
      </div>
    </n-alert>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
      <article class="nd-kpi-card">
        <p class="text-sm text-text-secondary">库存记录</p>
        <p class="nd-kpi-value text-2xl">{{ displayInventoryRows.length }}</p>
      </article>
      <article class="nd-kpi-card">
        <p class="text-sm text-text-secondary">低库存项</p>
        <p class="nd-kpi-value text-2xl">{{ alertRows.length }}</p>
      </article>
      <article class="nd-kpi-card">
        <p class="text-sm text-text-secondary">近期流水</p>
        <p class="nd-kpi-value text-2xl">{{ txnRows.length }}</p>
      </article>
    </div>

    <section class="nd-toolbar">
      <div class="nd-toolbar-group">
        <article class="nd-metric-chip">
          <p class="nd-metric-label">低库存占比</p>
          <p class="nd-metric-value">{{ displayInventoryRows.length > 0 ? Math.round((alertRows.length / displayInventoryRows.length) * 100) : 0 }}%</p>
        </article>
        <article class="nd-metric-chip">
          <p class="nd-metric-label">当前状态</p>
          <p class="nd-metric-value">{{ loading ? "同步中" : "就绪" }}</p>
        </article>
      </div>
      <div class="nd-status-strip">库存列表、预警列表与流水数据保持统一刷新</div>
    </section>
    <article class="nd-table-shell">
      <div class="nd-table-head">
        <h3 class="nd-section-title">低库存动作</h3>
      </div>
      <div class="nd-table-body text-xs text-text-secondary">
        优先读取商品规格中的“安全库存 XX”；若未配置，则使用默认阈值 10。页面预警与仪表盘低库存统计按同一口径计算。
      </div>
    </article>

    <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
      <article class="nd-table-shell">
        <div class="nd-table-head">
        <h3 class="nd-section-title">库存列表</h3>
        <p class="nd-section-subtitle">按仓库、库位和商品查看当前库存快照。</p>
        </div>
        <div class="nd-table-body">
        <n-data-table class="nd-table mt-3" :columns="inventoryColumns" :data="displayInventoryRows" :loading="loading" :bordered="false" />
        <n-empty v-if="!loading && displayInventoryRows.length === 0" class="nd-empty-shell mt-4" description="暂无库存记录" />
        </div>
      </article>

      <article class="nd-table-shell">
        <div class="nd-table-head">
        <h3 class="nd-section-title">低库存预警</h3>
        <p class="nd-section-subtitle">可优先处理可用库存偏低的商品</p>
        </div>
        <div class="nd-table-body">
        <n-data-table class="nd-table mt-3" :columns="alertColumns" :data="alertRows" :loading="loading" :bordered="false" />
        <n-empty v-if="!loading && alertRows.length === 0" class="nd-empty-shell mt-4" description="暂无低库存预警" />
        </div>
      </article>
    </div>

    <article class="nd-table-shell">
      <div class="nd-table-head">
      <h3 class="nd-section-title">库存流水</h3>
      <p class="nd-section-subtitle">支持追踪入库、出库带来的库存变化</p>
      </div>
      <div class="nd-table-body">
      <n-data-table class="nd-table mt-3" :columns="txnColumns" :data="txnRows" :loading="loading" :bordered="false" />
      <n-empty v-if="!loading && txnRows.length === 0" class="nd-empty-shell mt-4" description="暂无库存流水" />
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { NAlert, NButton, NDataTable, NEmpty, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { wmsApi, type InventoryItem, type InventoryTransaction, type Location, type Product, type Warehouse } from "@/services/wms";
import { agentApi } from "@/services/agent";
import { useAuthStore } from "@/stores/auth";

const message = useMessage();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const exporting = ref(false);
const smartLoading = ref("");
const errorText = ref("");
const lastSuccessText = ref("");
const inventoryRows = ref<InventoryItem[]>([]);
const alertRows = ref<InventoryItem[]>([]);
const txnRows = ref<InventoryTransaction[]>([]);
const warehouseRows = ref<Warehouse[]>([]);
const productRows = ref<Product[]>([]);
const locationRows = ref<Location[]>([]);
const dashboardHint = ref("");
const importInput = ref<HTMLInputElement | null>(null);

const displayInventoryRows = computed(() => {
  const from = String(route.query.from ?? "");
  const focus = String(route.query.focus ?? "");
  if (from === "dashboard" && focus === "low-stock") {
    return alertRows.value;
  }
  return inventoryRows.value;
});

const inventoryColumns: DataTableColumns<InventoryItem> = [
  { title: "ID", key: "id", width: 80 },
  { title: "仓库", key: "warehouseId", render: (row) => resolveWarehouseName(row.warehouseId) },
  { title: "库位", key: "locationId", render: (row) => resolveLocationName(row.locationId) },
  { title: "商品", key: "productId", render: (row) => resolveProductName(row.productId) },
  { title: "可用库存", key: "availableQty" },
  { title: "锁定库存", key: "lockedQty" },
  { title: "在途库存", key: "inTransitQty" }
];

const alertColumns: DataTableColumns<InventoryItem> = [
  { title: "商品", key: "productId", render: (row) => resolveProductName(row.productId) },
  { title: "仓库", key: "warehouseId", render: (row) => resolveWarehouseName(row.warehouseId) },
  { title: "库位", key: "locationId", render: (row) => resolveLocationName(row.locationId) },
  { title: "可用库存", key: "availableQty" }
];

const txnColumns: DataTableColumns<InventoryTransaction> = [
  { title: "流水号", key: "txnNo" },
  { title: "业务类型", key: "bizType" },
  { title: "业务单号", key: "bizNo" },
  { title: "商品", key: "productId", render: (row) => resolveProductName(row.productId) },
  { title: "变动数量", key: "changeQty" },
  { title: "结存", key: "afterQty" },
  { title: "时间", key: "occurredAt" }
];

function resolveWarehouseName(warehouseId: string) {
  const found = warehouseRows.value.find((item) => item.id === warehouseId);
  return found ? `${found.warehouseCode} / ${found.warehouseName}` : warehouseId;
}

function resolveLocationName(locationId: string) {
  const found = locationRows.value.find((item) => item.id === locationId);
  return found ? `${found.locationCode} / ${found.locationName}` : locationId;
}

function resolveProductName(productId: string) {
  const found = productRows.value.find((item) => item.id === productId);
  return found ? `${found.productCode} / ${found.productName}` : productId;
}

function applyRouteHint() {
  const from = String(route.query.from ?? "");
  const focus = String(route.query.focus ?? "");
  dashboardHint.value = from === "dashboard" && focus === "low-stock" ? "已应用低库存筛选" : "";
}

function downloadText(filename: string, content: string) {
  const blob = new Blob(["\uFEFF" + content], { type: "text/csv;charset=UTF-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

async function downloadTemplate() {
  try {
    downloadText("inventory-import-template.csv", await wmsApi.exportInventoryImportTemplate());
  } catch (error) {
    message.error(error instanceof Error ? error.message : "模板下载失败");
  }
}

async function downloadInventoryCsv() {
  exporting.value = true;
  errorText.value = "";
  lastSuccessText.value = "导出中，浏览器将开始下载。";
  try {
    await wmsApi.exportInventoryCsv();
    lastSuccessText.value = "库存 CSV 已触发下载";
    message.success(lastSuccessText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "库存导出失败";
    message.error(errorText.value);
  } finally {
    exporting.value = false;
  }
}

async function onImportFile(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;
  try {
    const result = await wmsApi.importInventory(await file.text());
    lastSuccessText.value = `CSV导入完成：成功 ${result.successRows}，失败 ${result.failedRows}`;
    if (result.reportId) {
      downloadText(`inventory-import-errors-${result.reportId}.csv`, await wmsApi.getInventoryImportErrorReport(result.reportId));
    }
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "CSV导入失败");
  } finally {
    if (importInput.value) importInput.value.value = "";
  }
}
async function clearDashboardFilter() {
  const nextQuery = { ...route.query };
  delete nextQuery.from;
  delete nextQuery.focus;
  await router.replace({ path: route.path, query: nextQuery });
}

async function loadData() {
  loading.value = true;
  errorText.value = "";
  try {
    const [inventory, alerts, transactions, warehouses, products, locations] = await Promise.all([
      wmsApi.listInventory(),
      wmsApi.listLowStockAlerts(),
      wmsApi.listInventoryTransactions(),
      wmsApi.listWarehouses(),
      wmsApi.listProducts(),
      wmsApi.listLocations()
    ]);
    inventoryRows.value = inventory;
    alertRows.value = alerts;
    txnRows.value = transactions;
    warehouseRows.value = warehouses;
    productRows.value = products;
    locationRows.value = locations;
    lastSuccessText.value = `库存数据已刷新：${new Date().toLocaleString("zh-CN", { hour12: false })}`;
    applyRouteHint();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "库存数据加载失败";
    message.error(errorText.value);
  } finally {
    loading.value = false;
  }
}

async function runSmartTask(taskCode: "LOW_STOCK_ANALYSIS" | "REPLENISH_SUGGESTION") {
  if (!authStore.hasPermission("AGENT_TASK_EXECUTE")) {
    message.warning("当前账号不能执行智能任务");
    return;
  }
  smartLoading.value = taskCode;
  try {
    const target = taskCode === "LOW_STOCK_ANALYSIS" ? { limit: 8, recentDays: 7 } : { limit: 8, lowStockThreshold: 10 };
    const run = await agentApi.executeTask(taskCode, target);
    message.success(`${run.taskName} 已生成`);
    await router.push({ path: "/agent/center", query: { runId: run.id } });
  } catch (error) {
    message.error(error instanceof Error ? error.message : "智能任务执行失败");
  } finally {
    smartLoading.value = "";
  }
}

onMounted(loadData);

watch(
  () => route.query,
  () => {
    applyRouteHint();
  },
  { deep: true }
);
</script>
