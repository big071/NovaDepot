<template>
  <section class="space-y-4">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">WMS</p>
          <h1 class="nd-page-title">入库单闭环管理</h1>
          <p class="nd-page-subtitle">手工入库与采购来源入库草稿、过账闭环。</p>
        </div>
        <div class="flex gap-2">
          <n-button class="nd-soft-focus" :loading="loading" @click="loadList">刷新</n-button>
          <n-button v-if="authStore.hasPermission('INBOUND_CREATE')" class="nd-soft-focus" type="primary" @click="createVisible = true">新建入库单</n-button>
        </div>
      </div>
    </header>

    <n-alert v-if="errorText" type="error" :show-icon="false">{{ errorText }}</n-alert>
    <n-alert v-else-if="successText" type="success" :show-icon="false">{{ successText }}</n-alert>

    <article class="nd-table-shell">
      <div class="nd-table-head"><h3 class="nd-section-title">入库单列表</h3></div>
      <div class="nd-table-body">
        <n-data-table :columns="columns" :data="rows" :loading="loading" :bordered="false" />
      </div>
    </article>

    <n-modal v-model:show="createVisible" preset="card" title="新建入库单" class="max-w-xl">
      <div class="space-y-3">
        <n-select v-model:value="createForm.warehouseId" :options="warehouseOptions" placeholder="选择仓库" @update:value="onWarehouseChange" />
        <n-input-number v-model:value="createForm.supplierId" :show-button="false" placeholder="供应商ID（可选）" />
        <n-select v-model:value="createForm.productId" :options="productOptions" placeholder="选择商品" />
        <n-select v-model:value="createForm.locationId" :options="locationOptions" placeholder="选择库位" />
        <n-input-number v-model:value="createForm.qty" :show-button="false" placeholder="数量" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button @click="createVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="onCreate">保存草稿</n-button>
        </div>
      </template>
    </n-modal>

    <n-modal v-model:show="detailVisible" preset="card" title="入库单详情" class="max-w-5xl">
      <div class="space-y-3">
        <n-descriptions bordered :column="3" size="small" label-placement="left" v-if="detail.order">
          <n-descriptions-item label="单号">{{ detail.order.inboundNo }}</n-descriptions-item>
          <n-descriptions-item label="Status">{{ statusText(detail.order.status) }}</n-descriptions-item>
          <n-descriptions-item label="来源">{{ sourceText(detail.order) }}</n-descriptions-item>
        </n-descriptions>
        <div class="nd-print-hidden flex justify-end"><n-button v-if="authStore.hasPermission('INBOUND_PRINT')" class="nd-soft-focus" @click="printCurrent">打印入库单</n-button></div>
        <div class="nd-print-sheet"><h2>入库单</h2><p>单号：{{ detail.order?.inboundNo }}</p><p>来源：{{ sourceText(detail.order) }}</p><table class="nd-print-table"><tbody><tr v-for="item in detail.items" :key="item.id"><td>{{ item.lineNo }}</td><td>{{ item.productId }}</td><td>{{ item.locationId }}</td><td>{{ item.planQty }}</td><td>{{ item.receivedQty }}</td></tr></tbody></table><p>仓库签字：________  经办人签字：________</p></div>
        <n-data-table :columns="timelineColumns" :data="detail.timeline" :bordered="false" size="small" />
        <n-data-table :columns="itemColumns" :data="detail.items" :bordered="false" size="small" />
      </div>
    </n-modal>
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from "vue";
import { NButton, NDataTable, NDescriptions, NDescriptionsItem, NInputNumber, NModal, NSelect, NTag, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { wmsApi, type InboundOrder, type InboundOrderItem, type Location, type OrderDetail, type OrderTimelineItem, type Product, type Warehouse } from "@/services/wms";
import { useAuthStore } from "@/stores/auth";

const ui = useMessage();
const authStore = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const errorText = ref("");
const successText = ref("");
const createVisible = ref(false);
const detailVisible = ref(false);
const rows = ref<InboundOrder[]>([]);
const detail = ref<OrderDetail<InboundOrder, InboundOrderItem>>({ order: {} as InboundOrder, items: [], timeline: [] });
const actionLoading = ref("");
const warehouses = ref<Warehouse[]>([]);
const products = ref<Product[]>([]);
const locations = ref<Location[]>([]);

const createForm = reactive({
  warehouseId: null as string | null,
  supplierId: null as number | null,
  productId: null as string | null,
  locationId: null as string | null,
  qty: 1
});

const warehouseOptions = computed(() => warehouses.value.map((v) => ({ label: `${v.warehouseCode} ${v.warehouseName}`, value: v.id })));
const productOptions = computed(() => products.value.map((v) => ({ label: `${v.productCode} ${v.productName}`, value: v.id })));
const locationOptions = computed(() => locations.value.map((v) => ({ label: `${v.locationCode} ${v.locationName}`, value: v.id })));

const columns: DataTableColumns<InboundOrder> = [
  { title: "单号", key: "inboundNo", minWidth: 180 },
  { title: "来源", key: "source", minWidth: 160, render: (row) => sourceText(row) },
  { title: "Status", key: "status", width: 120, render: (row) => h(NTag, { type: statusType(row.status), bordered: false }, { default: () => statusText(row.status) }) },
  {
    title: "操作",
    key: "actions",
    width: 560,
    render: (row) =>
      h("div", { class: "flex flex-wrap gap-2" }, [
        actionBtn("详情", () => openDetail(row.id), true, row.id),
        actionBtn("提交", () => runAction("submit", row), (row.status === "DRAFT" || row.status === "REJECTED") && authStore.hasPermission("INBOUND_SUBMIT"), row.id),
        actionBtn("撤回", () => runAction("withdraw", row), row.status === "SUBMITTED" && authStore.hasPermission("INBOUND_WITHDRAW"), row.id),
        actionBtn("审核", () => runAction("approve", row), row.status === "SUBMITTED" && authStore.hasPermission("INBOUND_APPROVE"), row.id),
        actionBtn("驳回", () => runAction("reject", row), row.status === "SUBMITTED" && authStore.hasPermission("INBOUND_APPROVE"), row.id),
        actionBtn("作废", () => runAction("cancel", row), (row.status === "DRAFT" || row.status === "SUBMITTED" || row.status === "REJECTED") && authStore.hasPermission("INBOUND_CANCEL"), row.id),
        actionBtn("过账", () => runAction("post", row), row.status === "APPROVED" && authStore.hasPermission("INBOUND_POST"), row.id, true)
      ])
  }
];

const itemColumns: DataTableColumns<InboundOrderItem> = [
  { title: "行号", key: "lineNo" },
  { title: "商品ID", key: "productId" },
  { title: "库位ID", key: "locationId" },
  { title: "计划数量", key: "planQty" },
  { title: "实收数量", key: "receivedQty" },
  { title: "合格数量", key: "qualifiedQty" }
];

const timelineColumns: DataTableColumns<OrderTimelineItem> = [
  { title: "时间", key: "occurredAt", width: 180 },
  { title: "操作人", key: "operatorName", width: 120 },
  { title: "动作", key: "actionLabel", width: 140 },
  { title: "状态变化", key: "status", render: (row) => `${row.statusFrom || "-"} -> ${row.statusTo || "-"}` },
  { title: "备注", key: "note" }
];

function actionBtn(label: string, onClick: () => void, enabled: boolean, rowId: string, primary = false) {
  return h(NButton, { size: "small", type: primary ? "primary" : "default", disabled: !enabled || Boolean(actionLoading.value), loading: actionLoading.value === `${rowId}:${label}`, onClick }, { default: () => label });
}

function statusType(status: string) {
  if (status === "POSTED") return "success";
  if (status === "APPROVED") return "info";
  if (status === "REJECTED" || status === "CANCELED") return "error";
  return "warning";
}

function statusText(status: string) {
  return ({ DRAFT: "DRAFT", SUBMITTED: "SUBMITTED", APPROVED: "APPROVED", REJECTED: "REJECTED", CANCELED: "CANCELED", POSTED: "POSTED" } as Record<string, string>)[status] || status;
}

function sourceText(row: InboundOrder) {
  if (row.sourceType === "PURCHASE_ORDER" && row.sourceOrderNo) return `采购：${row.sourceOrderNo}`;
  return "手工创建";
}

async function onWarehouseChange() {
  locations.value = createForm.warehouseId ? await wmsApi.listLocations(createForm.warehouseId, { force: true }) : [];
  createForm.locationId = locations.value[0]?.id || null;
}

async function loadMaster() {
  const [w, p] = await Promise.all([wmsApi.listWarehouses({ force: true }), wmsApi.listProducts({ force: true })]);
  warehouses.value = w;
  products.value = p;
  createForm.warehouseId = createForm.warehouseId || w[0]?.id || null;
  createForm.productId = createForm.productId || p[0]?.id || null;
  await onWarehouseChange();
}

async function loadList() {
  loading.value = true;
  errorText.value = "";
  try {
    rows.value = await wmsApi.listInboundOrders({ force: true });
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "入库单加载失败";
  } finally {
    loading.value = false;
  }
}

async function onCreate() {
  if (!createForm.warehouseId || !createForm.productId || !createForm.locationId || Number(createForm.qty) <= 0) {
    ui.warning("请补全仓库、商品、库位与数量");
    return;
  }
  submitting.value = true;
  try {
    await wmsApi.createInboundOrder({
      warehouseId: createForm.warehouseId,
      supplierId: createForm.supplierId || undefined,
      items: [{ productId: createForm.productId, locationId: createForm.locationId, qty: Number(createForm.qty) }]
    });
    createVisible.value = false;
    successText.value = "入库草稿已创建";
    await loadList();
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "创建失败";
  } finally {
    submitting.value = false;
  }
}

async function openDetail(id: string) {
  detail.value = await wmsApi.getInboundOrderDetail(id, { force: true });
  detailVisible.value = true;
}

function printCurrent() {
  window.print();
}

function askNote(action: string) {
  return window.prompt(`${action}备注（可留空）：`) || "";
}

async function runAction(action: "submit" | "withdraw" | "approve" | "reject" | "cancel" | "post", row: InboundOrder) {
  const note = askNote(action);
  actionLoading.value = `${row.id}:${action}`;
  try {
    if (action === "submit") await wmsApi.submitInboundOrder(row.id, note);
    if (action === "withdraw") await wmsApi.withdrawInboundOrder(row.id, note);
    if (action === "approve") await wmsApi.approveInboundOrder(row.id, note);
    if (action === "reject") await wmsApi.rejectInboundOrder(row.id, note);
    if (action === "cancel") await wmsApi.cancelInboundOrder(row.id, note);
    if (action === "post") await wmsApi.postInboundOrder(row.id, note);
    successText.value = "操作成功";
    await loadList();
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "操作失败";
  } finally {
    actionLoading.value = "";
  }
}

onMounted(async () => {
  await Promise.all([loadMaster(), loadList()]);
});
</script>
