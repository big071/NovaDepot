<template>
  <section class="space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Inventory</p>
          <h1 class="nd-page-title">搴撳瓨绠＄悊</h1>
          <p class="nd-page-subtitle">Inventory, low-stock alerts, CSV import, and movement history.</p>
        </div>
        <input ref="importInput" class="hidden" type="file" accept=".csv,text/csv" @change="onImportFile" />
        <n-button v-if="authStore.hasPermission('INVENTORY_TEMPLATE_EXPORT')" class="nd-soft-focus" @click="downloadTemplate">CSV模板</n-button>
        <n-button v-if="authStore.hasPermission('INVENTORY_IMPORT')" class="nd-soft-focus" @click="importInput?.click()">CSV导入</n-button>
        <n-button class="nd-soft-focus" :loading="loading" @click="loadData">鍒锋柊</n-button>
      </div>
      <div class="nd-hero-meta">
        <span class="nd-pill">搴撳瓨璁板綍锛歿{ displayInventoryRows.length }}</span>
        <span class="nd-pill">棰勮椤癸細{{ alertRows.length }}</span>
        <span class="nd-pill">娴佹按锛歿{ txnRows.length }}</span>
      </div>
    </header>
    <section class="nd-toolbar">
      <div class="nd-toolbar-group">
        <article class="nd-metric-chip">
          <p class="nd-metric-label">鏅鸿兘鍏ュ彛</p>
          <p class="nd-metric-value">Replenishment and risk analysis</p>
        </article>
      </div>
      <div class="flex items-center gap-2">
        <n-button class="nd-soft-focus" size="small" :loading="smartLoading === 'LOW_STOCK_ANALYSIS'" @click="runSmartTask('LOW_STOCK_ANALYSIS')">
          浣庡簱瀛樺垎鏋?
        </n-button>
        <n-button class="nd-soft-focus" size="small" :loading="smartLoading === 'REPLENISH_SUGGESTION'" @click="runSmartTask('REPLENISH_SUGGESTION')">
          鐢熸垚琛ヨ揣寤鸿
        </n-button>
      </div>
    </section>
    <n-alert class="nd-state-alert" type="info" :show-icon="false">
      鍏堢湅鈥滀綆搴撳瓨棰勮鈥濓紝鍐嶅喅瀹氣€滃叆搴撹ˉ璐р€濇垨鈥滆皟鏁村彂杩愪紭鍏堢骇鈥濄€傚簱瀛樺垪琛ㄣ€侀璀︿笌娴佹按浼氬悓姝ュ埛鏂般€?
    </n-alert>
    <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">
      <div class="flex items-center justify-between gap-2">
        <span>{{ errorText }}</span>
        <n-button text type="primary" @click="loadData">閲嶈瘯</n-button>
      </div>
    </n-alert>
    <n-alert v-else-if="lastSuccessText" class="nd-state-alert" type="success" :show-icon="false">{{ lastSuccessText }}</n-alert>

    <n-alert v-if="dashboardHint" class="nd-state-alert" type="info" :show-icon="false">
      <div class="flex items-center justify-between gap-2">
        <span>{{ dashboardHint }}</span>
        <n-button text type="primary" @click="clearDashboardFilter">Clear Filter</n-button>
      </div>
    </n-alert>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
      <article class="nd-kpi-card">
        <p class="text-sm text-text-secondary">Inventory records</p>
        <p class="nd-kpi-value text-2xl">{{ displayInventoryRows.length }}</p>
      </article>
      <article class="nd-kpi-card">
        <p class="text-sm text-text-secondary">浣庡簱瀛橀」</p>
        <p class="nd-kpi-value text-2xl">{{ alertRows.length }}</p>
      </article>
      <article class="nd-kpi-card">
        <p class="text-sm text-text-secondary">Recent transactions</p>
        <p class="nd-kpi-value text-2xl">{{ txnRows.length }}</p>
      </article>
    </div>

    <section class="nd-toolbar">
      <div class="nd-toolbar-group">
        <article class="nd-metric-chip">
          <p class="nd-metric-label">Low-stock ratio</p>
          <p class="nd-metric-value">{{ displayInventoryRows.length > 0 ? Math.round((alertRows.length / displayInventoryRows.length) * 100) : 0 }}%</p>
        </article>
        <article class="nd-metric-chip">
          <p class="nd-metric-label">Current status</p>
          <p class="nd-metric-value">{{ loading ? "Syncing" : "Ready" }}</p>
        </article>
      </div>
      <div class="nd-status-strip">搴撳瓨鍒楄〃銆侀璀﹀垪琛ㄤ笌娴佹按鏁版嵁淇濇寔缁熶竴鍒锋柊</div>
    </section>
    <article class="nd-table-shell">
      <div class="nd-table-head">
        <h3 class="nd-section-title">Low-stock Actions</h3>
      </div>
      <div class="nd-table-body text-xs text-text-secondary">
        浼樺厛璇诲彇鍟嗗搧瑙勬牸涓殑鈥滃畨鍏ㄥ簱瀛?XX鈥濓紱鑻ユ湭閰嶇疆锛屽垯浣跨敤榛樿闃堝€?10銆傞〉闈㈤璀︿笌浠〃鐩樹綆搴撳瓨缁熻鎸夊悓涓€鍙ｅ緞璁＄畻銆?
      </div>
    </article>

    <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
      <article class="nd-table-shell">
        <div class="nd-table-head">
        <h3 class="nd-section-title">搴撳瓨鍒楄〃</h3>
        <p class="nd-section-subtitle">Current inventory snapshot by warehouse, location, and product.</p>
        </div>
        <div class="nd-table-body">
        <n-data-table class="nd-table mt-3" :columns="inventoryColumns" :data="displayInventoryRows" :loading="loading" :bordered="false" />
        <n-empty v-if="!loading && displayInventoryRows.length === 0" class="nd-empty-shell mt-4" description="鏆傛棤搴撳瓨璁板綍" />
        </div>
      </article>

      <article class="nd-table-shell">
        <div class="nd-table-head">
        <h3 class="nd-section-title">Low-stock Alerts</h3>
        <p class="nd-section-subtitle">鍙紭鍏堝鐞嗗彲鐢ㄥ簱瀛樺亸浣庣殑鍟嗗搧</p>
        </div>
        <div class="nd-table-body">
        <n-data-table class="nd-table mt-3" :columns="alertColumns" :data="alertRows" :loading="loading" :bordered="false" />
        <n-empty v-if="!loading && alertRows.length === 0" class="nd-empty-shell mt-4" description="No low-stock alerts." />
        </div>
      </article>
    </div>

    <article class="nd-table-shell">
      <div class="nd-table-head">
      <h3 class="nd-section-title">Inventory Transactions</h3>
      <p class="nd-section-subtitle">鏀寔杩芥函鍏ュ簱銆佸嚭搴撳甫鏉ョ殑搴撳瓨鍙樺寲</p>
      </div>
      <div class="nd-table-body">
      <n-data-table class="nd-table mt-3" :columns="txnColumns" :data="txnRows" :loading="loading" :bordered="false" />
      <n-empty v-if="!loading && txnRows.length === 0" class="nd-empty-shell mt-4" description="鏆傛棤搴撳瓨娴佹按" />
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
  { title: "浠撳簱", key: "warehouseId", render: (row) => resolveWarehouseName(row.warehouseId) },
  { title: "搴撲綅", key: "locationId", render: (row) => resolveLocationName(row.locationId) },
  { title: "鍟嗗搧", key: "productId", render: (row) => resolveProductName(row.productId) },
  { title: "鍙敤搴撳瓨", key: "availableQty" },
  { title: "閿佸畾搴撳瓨", key: "lockedQty" },
  { title: "In Transit", key: "inTransitQty" }
];

const alertColumns: DataTableColumns<InventoryItem> = [
  { title: "鍟嗗搧", key: "productId", render: (row) => resolveProductName(row.productId) },
  { title: "浠撳簱", key: "warehouseId", render: (row) => resolveWarehouseName(row.warehouseId) },
  { title: "搴撲綅", key: "locationId", render: (row) => resolveLocationName(row.locationId) },
  { title: "鍙敤搴撳瓨", key: "availableQty" }
];

const txnColumns: DataTableColumns<InventoryTransaction> = [
  { title: "Txn No", key: "txnNo" },
  { title: "涓氬姟绫诲瀷", key: "bizType" },
  { title: "涓氬姟鍗曞彿", key: "bizNo" },
  { title: "鍟嗗搧", key: "productId", render: (row) => resolveProductName(row.productId) },
  { title: "Change Qty", key: "changeQty" },
  { title: "缁撳瓨", key: "afterQty" },
  { title: "鏃堕棿", key: "occurredAt" }
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
  dashboardHint.value = from === "dashboard" && focus === "low-stock" ? "Low-stock filter applied" : "";
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
    lastSuccessText.value = `搴撳瓨鏁版嵁宸插埛鏂帮細${new Date().toLocaleString("zh-CN", { hour12: false })}`;
    applyRouteHint();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "搴撳瓨鏁版嵁鍔犺浇澶辫触";
    message.error(errorText.value);
  } finally {
    loading.value = false;
  }
}

async function runSmartTask(taskCode: "LOW_STOCK_ANALYSIS" | "REPLENISH_SUGGESTION") {
  if (!authStore.hasPermission("AGENT_TASK_EXECUTE")) {
    message.warning("Current account cannot execute agent tasks");
    return;
  }
  smartLoading.value = taskCode;
  try {
    const target = taskCode === "LOW_STOCK_ANALYSIS" ? { limit: 8, recentDays: 7 } : { limit: 8, lowStockThreshold: 10 };
    const run = await agentApi.executeTask(taskCode, target);
    message.success(`${run.taskName} generated`);
    await router.push({ path: "/agent/center", query: { runId: run.id } });
  } catch (error) {
    message.error(error instanceof Error ? error.message : "鏅鸿兘浠诲姟鎵ц澶辫触");
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
