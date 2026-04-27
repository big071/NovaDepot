<template>
  <section class="space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">System</p>
          <h1 class="nd-page-title">审计中心</h1>
          <p class="nd-page-subtitle">按模块、动作、资源、业务编号、操作人和时间范围快速定位操作记录。</p>
        </div>
        <n-button class="nd-soft-focus" :loading="loading" @click="loadList">刷新</n-button>
      </div>
    </header>

    <n-alert class="nd-state-alert" type="info" :show-icon="false">
      筛选支持“可下拉选择 + 可输入搜索 + 可一键清空”。建议先设定时间范围，再按业务编号或操作人缩小范围。
    </n-alert>
    <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">{{ errorText }}</n-alert>

    <section class="nd-toolbar">
      <div class="nd-toolbar-group flex-wrap items-end">
        <div class="space-y-1">
          <p class="text-xs text-text-secondary">模块</p>
          <n-select
            v-model:value="filters.module"
            class="w-44 nd-soft-focus"
            :options="moduleOptions"
            clearable
            filterable
            tag
            placeholder="可下拉、可搜索"
          />
        </div>
        <div class="space-y-1">
          <p class="text-xs text-text-secondary">动作</p>
          <n-select
            v-model:value="filters.action"
            class="w-52 nd-soft-focus"
            :options="actionOptions"
            clearable
            filterable
            tag
            placeholder="可下拉、可搜索"
          />
        </div>
        <div class="space-y-1">
          <p class="text-xs text-text-secondary">资源类型</p>
          <n-select
            v-model:value="filters.resourceType"
            class="w-44 nd-soft-focus"
            :options="resourceTypeOptions"
            clearable
            filterable
            tag
            placeholder="可下拉、可搜索"
          />
        </div>
        <div class="space-y-1">
          <p class="text-xs text-text-secondary">资源ID</p>
          <n-input v-model:value="filters.resourceId" class="w-44 nd-soft-focus" placeholder="输入资源ID关键词" clearable />
        </div>
        <div class="space-y-1">
          <p class="text-xs text-text-secondary">操作人</p>
          <n-input v-model:value="filters.operatorKeyword" class="w-48 nd-soft-focus" placeholder="姓名或操作人ID" clearable />
        </div>
        <div class="space-y-1">
          <p class="text-xs text-text-secondary">业务编号</p>
          <n-input v-model:value="filters.bizNo" class="w-48 nd-soft-focus" placeholder="例如：IN-20260423-001" clearable />
        </div>
        <div class="space-y-1">
          <p class="text-xs text-text-secondary">时间范围</p>
          <n-date-picker
            v-model:value="filters.timeRange"
            class="w-72 nd-soft-focus"
            type="datetimerange"
            clearable
            value-format="timestamp"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />
        </div>
        <div class="space-y-1">
          <p class="text-xs text-text-secondary">仅失败</p>
          <div class="h-[34px] flex items-center rounded-md border border-border bg-surface px-2">
            <n-checkbox v-model:checked="filters.onlyFailed">只看失败动作</n-checkbox>
          </div>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <n-button class="nd-soft-focus" @click="resetFilters">清空</n-button>
        <n-button class="nd-soft-focus" type="primary" :loading="loading" @click="onSearch">查询</n-button>
      </div>
    </section>

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h3 class="nd-section-title">审计日志</h3>
          <p class="nd-section-subtitle">共 {{ total }} 条</p>
        </div>
      </div>
      <div class="nd-table-body">
        <n-data-table :columns="columns" :data="rows" :loading="loading" :bordered="false" />
        <n-empty v-if="!loading && rows.length === 0" class="nd-empty-shell mt-4" description="暂无审计数据" />
        <div v-if="total > 0" class="mt-4 flex justify-end">
          <n-pagination
            :page="pageNo"
            :page-size="pageSize"
            :item-count="total"
            :page-sizes="[20, 50, 100]"
            show-size-picker
            @update:page="onPageChange"
            @update:page-size="onPageSizeChange"
          />
        </div>
      </div>
    </article>

    <n-modal v-model:show="detailVisible" preset="card" title="审计详情" class="max-w-5xl">
      <div v-if="detail" class="space-y-4">
        <n-descriptions bordered :column="2" label-placement="left">
          <n-descriptions-item label="记录ID">{{ detail.id || "-" }}</n-descriptions-item>
          <n-descriptions-item label="发生时间">{{ detail.occurredAt || "-" }}</n-descriptions-item>
          <n-descriptions-item label="模块">{{ detail.module || "-" }}</n-descriptions-item>
          <n-descriptions-item label="动作">{{ detail.action || "-" }}</n-descriptions-item>
          <n-descriptions-item label="资源类型">{{ detail.resourceType || "-" }}</n-descriptions-item>
          <n-descriptions-item label="资源ID">{{ detail.resourceId || "-" }}</n-descriptions-item>
          <n-descriptions-item label="业务编号">{{ detail.bizNo || "-" }}</n-descriptions-item>
          <n-descriptions-item label="操作人">{{ formatOperator(detail) }}</n-descriptions-item>
        </n-descriptions>

        <article class="rounded-xl border border-border bg-bg/60 p-3">
          <h4 class="text-sm font-medium">字段变更对比（Diff）</h4>
          <n-data-table class="mt-2" :columns="diffColumns" :data="detail.diff ?? []" :bordered="false" :max-height="260" />
          <p v-if="(detail.diff ?? []).length === 0" class="mt-2 text-xs text-text-secondary">本条记录未提供字段级变更。</p>
        </article>

        <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <article class="rounded-xl border border-border bg-bg/60 p-3">
            <h4 class="text-sm font-medium">变更前</h4>
            <pre class="mt-2 whitespace-pre-wrap text-xs text-text-secondary">{{ prettyJson(detail.beforeObject ?? detail.beforeJson ?? {}) }}</pre>
          </article>
          <article class="rounded-xl border border-border bg-bg/60 p-3">
            <h4 class="text-sm font-medium">变更后</h4>
            <pre class="mt-2 whitespace-pre-wrap text-xs text-text-secondary">{{ prettyJson(detail.afterObject ?? detail.afterJson ?? {}) }}</pre>
          </article>
        </div>
      </div>
    </n-modal>
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import {
  NAlert,
  NButton,
  NCheckbox,
  NDataTable,
  NDatePicker,
  NDescriptions,
  NDescriptionsItem,
  NEmpty,
  NInput,
  NModal,
  NPagination,
  NSelect,
  useMessage
} from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { systemApi, type AuditLogDetail, type AuditLogDiffItem, type AuditLogItem } from "@/services/system";

const message = useMessage();
const route = useRoute();
const loading = ref(false);
const errorText = ref("");
const rows = ref<AuditLogItem[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(20);
const detailVisible = ref(false);
const detail = ref<AuditLogDetail | null>(null);

const filters = reactive({
  module: null as string | null,
  action: null as string | null,
  resourceType: null as string | null,
  resourceId: "",
  operatorKeyword: "",
  bizNo: "",
  onlyFailed: false,
  timeRange: null as [number, number] | null
});

const defaultModuleOptions = ["AUTH", "WMS", "CS", "AI", "AGENT", "SYSTEM"].map(toOption);
const defaultActionOptions = ["CREATE", "UPDATE", "APPROVE", "POST", "SHIP", "LOGIN_SUCCESS", "LOGIN_FAIL", "TASK_EXECUTE_SUCCESS", "TASK_EXECUTE_FAILED"].map(toOption);
const defaultResourceTypeOptions = ["USER", "PRODUCT", "WAREHOUSE", "LOCATION", "INBOUND_ORDER", "OUTBOUND_ORDER", "CS_TICKET", "AI_CONVERSATION", "AGENT_TASK_RUN"].map(toOption);

const moduleOptions = computed(() => mergeOptions(defaultModuleOptions, rows.value.map((row) => row.module)));
const actionOptions = computed(() => mergeOptions(defaultActionOptions, rows.value.map((row) => row.action)));
const resourceTypeOptions = computed(() => mergeOptions(defaultResourceTypeOptions, rows.value.map((row) => row.resourceType)));

const columns: DataTableColumns<AuditLogItem> = [
  { title: "模块", key: "module", width: 110 },
  { title: "动作", key: "action", width: 180 },
  { title: "资源类型", key: "resourceType", width: 150 },
  { title: "资源ID", key: "resourceId", width: 160, ellipsis: { tooltip: true } },
  { title: "业务编号", key: "bizNo", width: 180, render: (row) => row.bizNo || "-" },
  { title: "操作人", key: "operator", width: 180, render: (row) => formatOperator(row) },
  { title: "时间", key: "occurredAt", minWidth: 180 },
  {
    title: "操作",
    key: "actions",
    width: 100,
    render: (row) =>
      h(
        NButton,
        {
          class: "nd-soft-focus",
          size: "small",
          onClick: () => openDetail(row.id)
        },
        { default: () => "详情" }
      )
  }
];

const diffColumns: DataTableColumns<AuditLogDiffItem> = [
  { title: "字段", key: "field", width: 180 },
  { title: "变更前", key: "before", render: (row) => stringify(row.before) },
  { title: "变更后", key: "after", render: (row) => stringify(row.after) }
];

const queryPayload = computed(() => {
  const payload: Record<string, string | number | boolean | undefined> = {
    pageNo: pageNo.value,
    pageSize: pageSize.value,
    module: filters.module || undefined,
    action: filters.action || undefined,
    resourceType: filters.resourceType || undefined,
    resourceId: filters.resourceId.trim() || undefined,
    operatorKeyword: filters.operatorKeyword.trim() || undefined,
    bizNo: filters.bizNo.trim() || undefined,
    onlyFailed: filters.onlyFailed || undefined
  };
  if (filters.timeRange?.length === 2) {
    payload.dateFrom = toDateTime(filters.timeRange[0]);
    payload.dateTo = toDateTime(filters.timeRange[1]);
  }
  return payload;
});

function toOption(value: string) {
  return { label: value, value };
}

function mergeOptions(base: Array<{ label: string; value: string }>, values: Array<string | undefined>) {
  const set = new Set(base.map((item) => item.value));
  values.forEach((item) => {
    if (item) set.add(item);
  });
  return Array.from(set).map(toOption);
}

function stringify(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "string") return value;
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}

function prettyJson(value: unknown) {
  if (typeof value === "string") return value;
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

function formatOperator(row: Pick<AuditLogItem, "operatorName" | "operatorId">) {
  const name = row.operatorName || "";
  const id = row.operatorId ? String(row.operatorId) : "";
  if (name && id) return `${name}（${id}）`;
  return name || id || "-";
}

function toDateTime(timestamp: number) {
  const date = new Date(timestamp);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mm = String(date.getMinutes()).padStart(2, "0");
  const ss = String(date.getSeconds()).padStart(2, "0");
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`;
}

async function loadList() {
  loading.value = true;
  errorText.value = "";
  try {
    const resp = await systemApi.listAuditLogs(queryPayload.value);
    rows.value = resp.list ?? [];
    total.value = Number(resp.total ?? 0);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "审计日志加载失败";
    message.error(errorText.value);
  } finally {
    loading.value = false;
  }
}

async function openDetail(id: string) {
  try {
    detail.value = await systemApi.getAuditLogDetail(id);
    detailVisible.value = true;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审计详情加载失败");
  }
}

function resetFilters() {
  filters.module = null;
  filters.action = null;
  filters.resourceType = null;
  filters.resourceId = "";
  filters.operatorKeyword = "";
  filters.bizNo = "";
  filters.onlyFailed = false;
  filters.timeRange = null;
  pageNo.value = 1;
  loadList();
}

function onSearch() {
  pageNo.value = 1;
  loadList();
}

function onPageChange(nextPage: number) {
  pageNo.value = nextPage;
  loadList();
}

function onPageSizeChange(nextSize: number) {
  pageSize.value = nextSize;
  pageNo.value = 1;
  loadList();
}

function applyRouteFilters() {
  const q = route.query;
  if (typeof q.bizNo === "string" && q.bizNo.trim()) filters.bizNo = q.bizNo.trim();
  if (typeof q.resourceType === "string" && q.resourceType.trim()) filters.resourceType = q.resourceType.trim();
  if (typeof q.resourceId === "string" && q.resourceId.trim()) filters.resourceId = q.resourceId.trim();
}

onMounted(() => {
  applyRouteFilters();
  loadList();
});

watch(() => route.query, () => {
  applyRouteFilters();
  pageNo.value = 1;
  loadList();
});
</script>
