<template>
  <section class="space-y-5">
    <PageHeader eyebrow="ERP" title="采购管理" subtitle="维护采购草稿并确认采购单，后续 Sprint 再转入库。">
      <template #actions>
        <n-button class="nd-soft-focus" :loading="loading" @click="loadAll">刷新</n-button>
        <n-button v-if="canCreate" class="nd-soft-focus" type="primary" @click="openCreate">新增采购单</n-button>
      </template>
      <template #meta><span class="nd-pill">采购单：{{ rows.length }}</span></template>
    </PageHeader>

    <SearchForm :loading="loading" @search="loadAll" @reset="resetFilters">
      <n-select v-model:value="filters.status" :options="statusOptions" placeholder="状态" clearable />
      <n-select v-model:value="filters.partnerId" :options="supplierOptions" placeholder="供应商" clearable filterable />
    </SearchForm>

    <ErrorState v-if="errorText" :message="errorText" @retry="loadAll" />
    <n-alert v-else-if="successText" class="nd-state-alert" type="success" :show-icon="false">{{ successText }}</n-alert>

    <DataTable title="采购单列表" :subtitle="`共 ${rows.length} 条记录`" :columns="columns" :data="tableRows" :loading="loading">
      <template #empty><EmptyState description="暂无采购单。" /></template>
    </DataTable>

    <n-modal v-model:show="formVisible" preset="card" :title="editingId ? '编辑采购单' : '新增采购单'" class="max-w-3xl">
      <div class="space-y-3">
        <div class="grid gap-3 md:grid-cols-3">
          <n-select v-model:value="form.partnerId" :options="supplierOptions" placeholder="供应商" filterable />
          <n-select v-model:value="form.warehouseId" :options="warehouseOptions" placeholder="收货仓库" filterable />
          <n-input v-model:value="form.expectedArrivalDate" placeholder="预计到货日期 yyyy-MM-dd" />
        </div>
        <n-input v-model:value="form.remark" placeholder="备注" />
        <div class="space-y-2">
          <div v-for="(item, index) in form.items" :key="index" class="grid gap-2 rounded-lg border border-border p-3 md:grid-cols-[1fr_120px_120px_80px]">
            <n-select v-model:value="item.productId" :options="productOptions" placeholder="商品" filterable />
            <n-input-number v-model:value="item.orderQty" :min="0.000001" placeholder="数量" />
            <n-input-number v-model:value="item.unitPrice" :min="0" placeholder="单价" />
            <n-button class="nd-soft-focus" tertiary @click="removeItem(index)">删除</n-button>
          </div>
          <n-button class="nd-soft-focus" @click="addItem">添加明细</n-button>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" @click="formVisible = false">取消</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="submitting" :disabled="!canSubmit" @click="save">保存</n-button>
        </div>
      </template>
    </n-modal>

    <n-modal v-model:show="detailVisible" preset="card" title="采购单详情" class="max-w-3xl">
      <n-descriptions v-if="detail" bordered :column="2">
        <n-descriptions-item label="单号">{{ detail.order.purchaseNo }}</n-descriptions-item>
        <n-descriptions-item label="状态"><StatusBadge :status="detail.order.status" /></n-descriptions-item>
        <n-descriptions-item label="供应商">{{ partnerName(detail.order.partnerId) }}</n-descriptions-item>
        <n-descriptions-item label="总金额">{{ detail.order.totalAmount }}</n-descriptions-item>
      </n-descriptions>
      <n-data-table v-if="detail" class="mt-4" :columns="itemColumns" :data="detail.items" :bordered="false" />
    </n-modal>

    <ConfirmDialog
      v-model:show="confirmVisible"
      :title="pendingAction === 'confirm' ? '确认采购单' : '取消采购单'"
      :content="pendingAction === 'confirm' ? '确认后采购单将进入已确认状态，Sprint 1 不会生成入库单。' : '取消后采购单不能继续编辑或确认。'"
      positive-text="确认"
      @confirm="applyOrderAction"
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
import { erpApi, type ErpOrder, type ErpOrderDetail, type Partner } from "@/services/erp";
import { wmsApi, type Product, type Warehouse } from "@/services/wms";
import { useAuthStore } from "@/stores/auth";

type OrderAction = "confirm" | "cancel";

const message = useMessage();
const authStore = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const errorText = ref("");
const successText = ref("");
const formVisible = ref(false);
const detailVisible = ref(false);
const confirmVisible = ref(false);
const editingId = ref<string | null>(null);
const pendingOrderId = ref<string | null>(null);
const pendingAction = ref<OrderAction>("confirm");
const rows = ref<ErpOrder[]>([]);
const partners = ref<Partner[]>([]);
const products = ref<Product[]>([]);
const warehouses = ref<Warehouse[]>([]);
const detail = ref<ErpOrderDetail | null>(null);

const filters = reactive({
  status: null as string | null,
  partnerId: null as string | null
});

const form = reactive({
  partnerId: null as string | null,
  warehouseId: null as string | null,
  expectedArrivalDate: "",
  remark: "",
  items: [{ productId: null as string | null, orderQty: 1, unitPrice: 0 }]
});

const statusOptions = [
  { label: "草稿", value: "DRAFT" },
  { label: "已确认", value: "CONFIRMED" },
  { label: "已取消", value: "CANCELLED" }
];

const canCreate = computed(() => authStore.hasPermission("PURCHASE_CREATE"));
const canUpdate = computed(() => authStore.hasPermission("PURCHASE_UPDATE"));
const canConfirm = computed(() => authStore.hasPermission("PURCHASE_CONFIRM"));
const canCancel = computed(() => authStore.hasPermission("PURCHASE_CANCEL"));
const canSubmit = computed(() => Boolean(form.partnerId && form.warehouseId && form.items.length && form.items.every((item) => item.productId && item.orderQty > 0 && item.unitPrice >= 0)) && !submitting.value);
const tableRows = computed(() => rows.value as unknown as Array<Record<string, unknown>>);
const supplierOptions = computed(() => partners.value.filter((p) => p.partnerType === "SUPPLIER" || p.partnerType === "BOTH").map((p) => ({ label: p.partnerName, value: p.id })));
const productOptions = computed(() => products.value.map((p) => ({ label: `${p.productCode} ${p.productName}`, value: p.id })));
const warehouseOptions = computed(() => warehouses.value.map((w) => ({ label: w.warehouseName, value: w.id })));

const columns: DataTableColumns<Record<string, unknown>> = [
  { title: "采购单号", key: "purchaseNo" },
  { title: "供应商", key: "partnerId", render: (row) => partnerName(String(row.partnerId || "")) },
  { title: "状态", key: "status", render: (row) => h(StatusBadge, { status: String(row.status || "") }) },
  { title: "总金额", key: "totalAmount" },
  { title: "预计到货", key: "expectedArrivalDate", render: (row) => String(row.expectedArrivalDate || "-") },
  {
    title: "操作",
    key: "actions",
    width: 240,
    render: (row) => {
      const order = row as unknown as ErpOrder;
      return h("div", { class: "flex flex-wrap gap-2" }, [
        h(NButton, { class: "nd-soft-focus", size: "small", onClick: () => openDetail(order.id) }, { default: () => "详情" }),
        canUpdate.value && order.status === "DRAFT" ? h(NButton, { class: "nd-soft-focus", size: "small", onClick: () => openEdit(order.id) }, { default: () => "编辑" }) : null,
        canConfirm.value && order.status === "DRAFT" ? h(NButton, { class: "nd-soft-focus", size: "small", type: "primary", onClick: () => openOrderConfirm(order.id, "confirm") }, { default: () => "确认" }) : null,
        canCancel.value && order.status !== "CANCELLED" ? h(NButton, { class: "nd-soft-focus", size: "small", type: "warning", onClick: () => openOrderConfirm(order.id, "cancel") }, { default: () => "取消" }) : null
      ]);
    }
  }
];

const itemColumns: DataTableColumns<ErpOrderDetail["items"][number]> = [
  { title: "行号", key: "lineNo" },
  { title: "商品", key: "productId", render: (row) => productName(row.productId) },
  { title: "数量", key: "orderQty" },
  { title: "单价", key: "unitPrice" }
];

function partnerName(id?: string) {
  return partners.value.find((p) => String(p.id) === String(id))?.partnerName || "-";
}

function productName(id?: string) {
  return products.value.find((p) => String(p.id) === String(id))?.productName || "-";
}

function resetFilters() {
  filters.status = null;
  filters.partnerId = null;
  loadAll();
}

function resetForm() {
  editingId.value = null;
  form.partnerId = null;
  form.warehouseId = null;
  form.expectedArrivalDate = "";
  form.remark = "";
  form.items = [{ productId: null, orderQty: 1, unitPrice: 0 }];
}

function addItem() {
  form.items.push({ productId: null, orderQty: 1, unitPrice: 0 });
}

function removeItem(index: number) {
  if (form.items.length > 1) form.items.splice(index, 1);
}

function openCreate() {
  resetForm();
  formVisible.value = true;
}

async function openEdit(id: string) {
  const data = await erpApi.getPurchaseOrder(id);
  editingId.value = id;
  form.partnerId = String(data.order.partnerId);
  form.warehouseId = String(data.order.warehouseId);
  form.expectedArrivalDate = data.order.expectedArrivalDate || "";
  form.remark = data.order.remark || "";
  form.items = data.items.map((item) => ({ productId: String(item.productId), orderQty: Number(item.orderQty), unitPrice: Number(item.unitPrice) }));
  formVisible.value = true;
}

async function openDetail(id: string) {
  detail.value = await erpApi.getPurchaseOrder(id);
  detailVisible.value = true;
}

function openOrderConfirm(id: string, action: OrderAction) {
  pendingOrderId.value = id;
  pendingAction.value = action;
  confirmVisible.value = true;
}

async function loadAll() {
  loading.value = true;
  errorText.value = "";
  try {
    const [orderRows, partnerRows, productRows, warehouseRows] = await Promise.all([
      erpApi.listPurchaseOrders({ status: filters.status || undefined, partnerId: filters.partnerId || undefined }),
      erpApi.listPartners(),
      wmsApi.listProducts({ force: true }),
      wmsApi.listWarehouses({ force: true })
    ]);
    rows.value = orderRows;
    partners.value = partnerRows;
    products.value = productRows;
    warehouses.value = warehouseRows;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "采购数据加载失败";
    message.error(errorText.value);
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (!canSubmit.value) return;
  submitting.value = true;
  errorText.value = "";
  try {
    const payload = {
      partnerId: Number(form.partnerId),
      warehouseId: Number(form.warehouseId),
      expectedArrivalDate: form.expectedArrivalDate || undefined,
      remark: form.remark || undefined,
      items: form.items.map((item) => ({ productId: Number(item.productId), orderQty: Number(item.orderQty), unitPrice: Number(item.unitPrice) }))
    };
    if (editingId.value) {
      await erpApi.updatePurchaseOrder(editingId.value, payload);
      successText.value = "采购单已更新";
    } else {
      await erpApi.createPurchaseOrder(payload);
      successText.value = "采购单已创建";
    }
    formVisible.value = false;
    await loadAll();
    message.success(successText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "保存失败";
    message.error(errorText.value);
  } finally {
    submitting.value = false;
  }
}

async function applyOrderAction() {
  if (!pendingOrderId.value) return;
  confirmVisible.value = false;
  try {
    if (pendingAction.value === "confirm") {
      await erpApi.confirmPurchaseOrder(pendingOrderId.value);
      successText.value = "采购单已确认";
    } else {
      await erpApi.cancelPurchaseOrder(pendingOrderId.value);
      successText.value = "采购单已取消";
    }
    await loadAll();
    message.success(successText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "状态更新失败";
    message.error(errorText.value);
  } finally {
    pendingOrderId.value = null;
  }
}

onMounted(loadAll);
</script>
