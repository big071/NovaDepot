<template>
  <section class="space-y-5">
    <PageHeader eyebrow="WMS" title="Stocktake" subtitle="Create stocktake orders, count physical quantity, and confirm differences through inventory adjustment transactions.">
      <template #actions>
        <n-button class="nd-soft-focus" :loading="loading" @click="loadAll">Refresh</n-button>
        <n-button v-if="canCreate" class="nd-soft-focus" type="primary" @click="openCreate">New stocktake</n-button>
      </template>
      <template #meta>
        <span class="nd-pill">Orders: {{ rows.length }}</span>
      </template>
    </PageHeader>

    <SearchForm :loading="loading" @search="loadAll" @reset="resetFilters">
      <n-select v-model:value="filters.status" :options="statusOptions" placeholder="Status" clearable />
    </SearchForm>

    <ErrorState v-if="errorText" :message="errorText" @retry="loadAll" />
    <n-alert v-else-if="successText" class="nd-state-alert" type="success" :show-icon="false">{{ successText }}</n-alert>

    <DataTable title="Stocktake orders" :subtitle="`Total ${filteredRows.length}`" :columns="columns" :data="filteredRows" :loading="loading">
      <template #empty><EmptyState description="No stocktake orders" /></template>
    </DataTable>

    <n-modal v-model:show="createVisible" preset="card" title="New stocktake" class="max-w-xl">
      <div class="space-y-3">
        <n-select v-model:value="form.warehouseId" :options="warehouseOptions" placeholder="Warehouse" filterable />
        <n-input v-model:value="form.remark" placeholder="Remark" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" @click="createVisible = false">Cancel</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="submitting" :disabled="!form.warehouseId" @click="createOrder">Create</n-button>
        </div>
      </template>
    </n-modal>

    <n-modal v-model:show="detailVisible" preset="card" title="Stocktake detail" class="max-w-5xl">
      <div v-if="detail" class="space-y-4">
        <n-descriptions bordered :column="2">
          <n-descriptions-item label="No">{{ detail.order.stocktakeNo }}</n-descriptions-item>
          <n-descriptions-item label="Status"><StatusBadge :status="detail.order.status" /></n-descriptions-item>
          <n-descriptions-item label="Warehouse">{{ warehouseName(detail.order.warehouseId) }}</n-descriptions-item>
          <n-descriptions-item label="Diff count">{{ detail.order.diffCount || 0 }}</n-descriptions-item>
        </n-descriptions>
        <n-data-table :columns="itemColumns" :data="detail.items" :bordered="false" />
      </div>
    </n-modal>

    <ConfirmDialog
      v-model:show="confirmVisible"
      :title="confirmTitle"
      :content="confirmContent"
      positive-text="Confirm"
      @confirm="applyPendingAction"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from "vue";
import { NAlert, NButton, NDataTable, NDescriptions, NDescriptionsItem, NInput, NInputNumber, NModal, NSelect, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import PageHeader from "@/components/shared/PageHeader.vue";
import DataTable from "@/components/shared/DataTable.vue";
import SearchForm from "@/components/shared/SearchForm.vue";
import StatusBadge from "@/components/shared/StatusBadge.vue";
import ConfirmDialog from "@/components/shared/ConfirmDialog.vue";
import EmptyState from "@/components/shared/EmptyState.vue";
import ErrorState from "@/components/shared/ErrorState.vue";
import { wmsApi, type Product, type StocktakeDetail, type StocktakeItem, type StocktakeOrder, type Warehouse, type Location } from "@/services/wms";
import { useAuthStore } from "@/stores/auth";

type PendingAction = "start" | "submit" | "confirm";

const message = useMessage();
const authStore = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const errorText = ref("");
const successText = ref("");
const createVisible = ref(false);
const detailVisible = ref(false);
const confirmVisible = ref(false);
const rows = ref<StocktakeOrder[]>([]);
const warehouses = ref<Warehouse[]>([]);
const products = ref<Product[]>([]);
const locations = ref<Location[]>([]);
const detail = ref<StocktakeDetail | null>(null);
const pendingId = ref("");
const pendingAction = ref<PendingAction>("start");
const filters = reactive({ status: null as string | null });
const form = reactive({ warehouseId: null as string | null, remark: "" });

const canCreate = computed(() => authStore.hasPermission("STOCKTAKE_CREATE"));
const canCount = computed(() => authStore.hasPermission("STOCKTAKE_COUNT"));
const canConfirm = computed(() => authStore.hasPermission("STOCKTAKE_CONFIRM"));
const statusOptions = ["DRAFT", "IN_PROGRESS", "DIFF_REVIEW", "COMPLETED"].map((value) => ({ label: value, value }));
const warehouseOptions = computed(() => warehouses.value.map((item) => ({ label: `${item.warehouseCode} ${item.warehouseName}`, value: item.id })));
const filteredRows = computed(() => rows.value.filter((row) => !filters.status || row.status === filters.status) as unknown as Array<Record<string, unknown>>);
const confirmTitle = computed(() => {
  if (pendingAction.value === "start") return "Start stocktake";
  if (pendingAction.value === "submit") return "Submit differences";
  return "Confirm adjustment";
});
const confirmContent = computed(() => {
  if (pendingAction.value === "start") return "A stock snapshot will be created from current inventory.";
  if (pendingAction.value === "submit") return "All counted quantities will be submitted for difference review.";
  return "Difference confirmation writes inventory adjustment transactions and updates stock.";
});

const columns: DataTableColumns<Record<string, unknown>> = [
  { title: "No", key: "stocktakeNo" },
  { title: "Warehouse", key: "warehouseId", render: (row) => warehouseName(String(row.warehouseId || "")) },
  { title: "Status", key: "status", render: (row) => h(StatusBadge, { status: String(row.status || "") }) },
  { title: "Diff", key: "diffCount" },
  { title: "Started", key: "startedAt", render: (row) => String(row.startedAt || "-") },
  {
    title: "Actions",
    key: "actions",
    width: 340,
    render: (row) => {
      const order = row as unknown as StocktakeOrder;
      return h("div", { class: "flex flex-wrap gap-2" }, [
        h(NButton, { class: "nd-soft-focus", size: "small", onClick: () => openDetail(order.id) }, { default: () => "Detail" }),
        canCount.value && order.status === "DRAFT" ? h(NButton, { class: "nd-soft-focus", size: "small", type: "primary", onClick: () => openAction(order.id, "start") }, { default: () => "Start" }) : null,
        canCount.value && order.status === "IN_PROGRESS" ? h(NButton, { class: "nd-soft-focus", size: "small", onClick: () => openDetail(order.id) }, { default: () => "Count" }) : null,
        canCount.value && order.status === "IN_PROGRESS" ? h(NButton, { class: "nd-soft-focus", size: "small", type: "primary", onClick: () => openAction(order.id, "submit") }, { default: () => "Submit" }) : null,
        canConfirm.value && order.status === "DIFF_REVIEW" ? h(NButton, { class: "nd-soft-focus", size: "small", type: "primary", onClick: () => openAction(order.id, "confirm") }, { default: () => "Confirm" }) : null
      ]);
    }
  }
];

const itemColumns: DataTableColumns<StocktakeItem> = [
  { title: "Line", key: "lineNo" },
  { title: "Product", key: "productId", render: (row) => productName(row.productId) },
  { title: "Location", key: "locationId", render: (row) => locationName(row.locationId) },
  { title: "System", key: "systemQty" },
  {
    title: "Counted",
    key: "countedQty",
    render: (row) =>
      detail.value?.order.status === "IN_PROGRESS" && canCount.value
        ? h(NInputNumber, {
            value: row.countedQty,
            min: 0,
            onUpdateValue: (value: number | null) => updateCount(row.id, value ?? 0)
          })
        : row.countedQty
  },
  { title: "Diff", key: "diffQty" },
  { title: "Result", key: "resultType" }
];

function warehouseName(id: string) {
  return warehouses.value.find((item) => String(item.id) === String(id))?.warehouseName || id || "-";
}

function productName(id: string) {
  const item = products.value.find((product) => String(product.id) === String(id));
  return item ? `${item.productCode} ${item.productName}` : id || "-";
}

function locationName(id: string) {
  const item = locations.value.find((location) => String(location.id) === String(id));
  return item ? `${item.locationCode} ${item.locationName}` : id || "-";
}

function resetFilters() {
  filters.status = null;
  loadAll();
}

function openCreate() {
  form.warehouseId = warehouses.value[0]?.id ?? null;
  form.remark = "";
  createVisible.value = true;
}

function openAction(id: string, action: PendingAction) {
  pendingId.value = id;
  pendingAction.value = action;
  confirmVisible.value = true;
}

async function applyPendingAction() {
  confirmVisible.value = false;
  if (!pendingId.value) return;
  submitting.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    if (pendingAction.value === "start") {
      await wmsApi.startStocktake(pendingId.value);
      successText.value = "Stocktake started.";
    } else if (pendingAction.value === "submit") {
      await wmsApi.submitStocktakeReview(pendingId.value);
      successText.value = "Stocktake submitted for review.";
    } else {
      await wmsApi.confirmStocktake(pendingId.value);
      successText.value = "Stocktake differences confirmed.";
    }
    await loadAll();
    if (detail.value?.order.id === pendingId.value) await openDetail(pendingId.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "Stocktake action failed";
    message.error(errorText.value);
  } finally {
    submitting.value = false;
  }
}

async function createOrder() {
  if (!form.warehouseId) return;
  submitting.value = true;
  errorText.value = "";
  try {
    await wmsApi.createStocktake({ warehouseId: form.warehouseId, remark: form.remark || undefined });
    createVisible.value = false;
    successText.value = "Stocktake created.";
    await loadAll();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "Failed to create stocktake";
  } finally {
    submitting.value = false;
  }
}

async function openDetail(id: string) {
  detail.value = await wmsApi.getStocktake(id);
  detailVisible.value = true;
}

async function updateCount(itemId: string, value: number) {
  if (!detail.value) return;
  const orderId = detail.value.order.id;
  await wmsApi.updateStocktakeCount(orderId, itemId, value);
  detail.value = await wmsApi.getStocktake(orderId);
  successText.value = "Count updated.";
}

async function loadAll() {
  loading.value = true;
  errorText.value = "";
  try {
    const [stocktakes, warehouseRows, productRows, locationRows] = await Promise.all([
      wmsApi.listStocktakes(),
      wmsApi.listWarehouses(),
      wmsApi.listProducts(),
      wmsApi.listLocations()
    ]);
    rows.value = stocktakes;
    warehouses.value = warehouseRows;
    products.value = productRows;
    locations.value = locationRows;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "Failed to load stocktakes";
  } finally {
    loading.value = false;
  }
}

onMounted(loadAll);
</script>
