<template>
  <section class="space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Reports</p>
          <h1 class="nd-page-title">报表中心</h1>
          <p class="nd-page-subtitle">固定运营报表覆盖库存、出入库、购销和工单效率，支持 CSV 导出。</p>
        </div>
        <div class="flex gap-2">
          <n-button class="nd-soft-focus" :loading="loading" @click="loadReport">刷新</n-button>
          <n-button class="nd-soft-focus" type="primary" :disabled="!canExport" @click="exportCsv">CSV 导出</n-button>
        </div>
      </div>
    </header>

    <n-alert v-if="errorText" type="error" :show-icon="false">
      {{ errorText }}
      <n-button size="small" class="ml-2" @click="loadReport">重试</n-button>
    </n-alert>

    <section class="nd-toolbar">
      <div class="nd-toolbar-group flex-wrap items-end">
        <div class="space-y-1">
          <p class="text-xs text-text-secondary">时间范围</p>
          <n-date-picker v-model:value="rangeValue" class="w-72 nd-soft-focus" type="daterange" clearable />
        </div>
        <div v-if="activeTab === 'inout'" class="space-y-1">
          <p class="text-xs text-text-secondary">粒度</p>
          <n-select v-model:value="grain" class="w-36 nd-soft-focus" :options="grainOptions" />
        </div>
      </div>
    </section>

    <n-tabs v-model:value="activeTab" type="line" animated @update:value="loadReport">
      <n-tab-pane name="inventory" tab="库存周转" />
      <n-tab-pane name="inout" tab="出入库日报/周报" />
      <n-tab-pane name="purchaseSales" tab="购销汇总" />
      <n-tab-pane name="tickets" tab="工单效率" />
    </n-tabs>

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h3 class="nd-section-title">{{ currentTitle }}</h3>
          <p class="nd-section-subtitle">共 {{ rows.length }} 条，{{ dateLabel }}</p>
        </div>
      </div>
      <div class="nd-table-body space-y-4">
        <div v-if="loading" class="space-y-3">
          <n-skeleton height="76px" />
          <n-skeleton v-for="i in 4" :key="i" height="42px" />
        </div>
        <template v-else>
          <div class="grid gap-2 md:grid-cols-4">
            <article v-for="item in chartRows" :key="item.label" class="rounded-xl border border-border bg-bg/60 p-3">
              <p class="truncate text-xs text-text-secondary">{{ item.label }}</p>
              <div class="mt-2 h-2 rounded-full bg-border">
                <div class="h-2 rounded-full bg-primary" :style="{ width: item.width }" />
              </div>
              <p class="mt-2 text-sm font-semibold">{{ item.value }}</p>
            </article>
          </div>
          <n-data-table :columns="columns" :data="rows" :bordered="false" />
          <n-empty v-if="rows.length === 0" class="nd-empty-shell" description="暂无报表数据" />
        </template>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { NAlert, NButton, NDataTable, NDatePicker, NEmpty, NSelect, NSkeleton, NTabPane, NTabs, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { reportsApi, type ReportQuery } from "@/services/reports";
import { useAuthStore } from "@/stores/auth";

type ReportTab = "inventory" | "inout" | "purchaseSales" | "tickets";
type Row = Record<string, unknown>;

const authStore = useAuthStore();
const message = useMessage();
const loading = ref(false);
const errorText = ref("");
const activeTab = ref<ReportTab>("inventory");
const rangeValue = ref<[number, number] | null>(null);
const grain = ref<"DAY" | "WEEK">("DAY");
const rows = ref<Row[]>([]);
const dateFrom = ref("");
const dateTo = ref("");

const grainOptions = [
  { label: "日", value: "DAY" },
  { label: "周", value: "WEEK" }
];

const canExport = computed(() => authStore.hasPermission("REPORT_EXPORT"));
const currentTitle = computed(() => {
  if (activeTab.value === "inventory") return "库存周转";
  if (activeTab.value === "inout") return "出入库日报/周报";
  if (activeTab.value === "purchaseSales") return "购销汇总";
  return "工单处理效率";
});
const dateLabel = computed(() => (dateFrom.value && dateTo.value ? `${dateFrom.value} 至 ${dateTo.value}` : "默认近 7 天"));

const columns = computed<DataTableColumns<Row>>(() => {
  if (activeTab.value === "inventory") {
    return [
      { title: "商品", key: "productName", minWidth: 220 },
      { title: "出库数量", key: "outboundQty", width: 140 },
      { title: "可用库存", key: "availableQty", width: 140 },
      { title: "周转率", key: "turnoverRate", width: 120 }
    ];
  }
  if (activeTab.value === "inout") {
    return [
      { title: "周期", key: "period", width: 160 },
      { title: "入库单数", key: "inboundCount", width: 140 },
      { title: "出库单数", key: "outboundCount", width: 140 },
      { title: "净变化", key: "netCount", width: 140 }
    ];
  }
  if (activeTab.value === "purchaseSales") {
    return [
      { title: "类型", key: "type", width: 140 },
      { title: "单数", key: "count", width: 120 },
      { title: "金额", key: "amount", width: 160 }
    ];
  }
  return [
    { title: "负责人", key: "assigneeId", width: 160 },
    { title: "工单数", key: "ticketCount", width: 140 },
    { title: "已解决", key: "closedCount", width: 140 },
    { title: "关闭率", key: "closeRate", width: 140 }
  ];
});

const chartRows = computed(() => {
  const values = rows.value.slice(0, 4).map((row) => {
    const label = String(row.productName ?? row.period ?? row.type ?? row.assigneeId ?? "-");
    const value = Number(row.outboundQty ?? row.inboundCount ?? row.amount ?? row.ticketCount ?? 0);
    return { label, value };
  });
  const max = Math.max(1, ...values.map((item) => item.value));
  return values.map((item) => ({ ...item, width: `${Math.max(6, Math.round((item.value / max) * 100))}%` }));
});

function buildQuery(): ReportQuery {
  const query: ReportQuery = {};
  if (rangeValue.value) {
    query.dateFrom = toDate(rangeValue.value[0]);
    query.dateTo = toDate(rangeValue.value[1]);
  }
  if (activeTab.value === "inout") {
    query.grain = grain.value;
  }
  return query;
}

async function loadReport() {
  loading.value = true;
  errorText.value = "";
  try {
    const query = buildQuery();
    const resp =
      activeTab.value === "inventory"
        ? await reportsApi.inventoryTurnover(query)
        : activeTab.value === "inout"
          ? await reportsApi.inoutSummary(query)
          : activeTab.value === "purchaseSales"
            ? await reportsApi.purchaseSalesSummary(query)
            : await reportsApi.ticketEfficiency(query);
    rows.value = resp.rows as unknown as Row[];
    dateFrom.value = resp.dateFrom;
    dateTo.value = resp.dateTo;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "报表加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

async function exportCsv() {
  try {
    const query = buildQuery();
    if (activeTab.value === "inventory") await reportsApi.exportInventoryTurnover(query);
    if (activeTab.value === "inout") await reportsApi.exportInoutSummary(query);
    if (activeTab.value === "purchaseSales") await reportsApi.exportPurchaseSalesSummary(query);
    if (activeTab.value === "tickets") await reportsApi.exportTicketEfficiency(query);
    message.success("CSV 已开始下载");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "CSV 导出失败");
  }
}

function toDate(value: number) {
  const date = new Date(value);
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

watch([rangeValue, grain], loadReport);
onMounted(loadReport);
</script>
