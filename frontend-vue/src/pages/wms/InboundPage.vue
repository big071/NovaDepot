<template>
  <section class="space-y-4">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">WMS</p>
          <h1 class="nd-page-title">入库单闭环管理</h1>
          <p class="nd-page-subtitle">支持提交备注、审核意见、驳回原因、撤回原因、作废原因和过账备注，并可回看完整处理历史。</p>
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
      <div class="nd-table-head">
        <h3 class="nd-section-title">入库单列表</h3>
      </div>
      <div class="nd-table-body">
        <n-data-table :columns="columns" :data="rows" :loading="loading" :bordered="false" />
      </div>
    </article>

    <n-modal v-model:show="createVisible" preset="card" title="新建入库单" class="max-w-xl">
      <div class="space-y-3">
        <n-select v-model:value="createForm.warehouseId" :options="warehouseOptions" placeholder="选择仓库" @update:value="onWarehouseChange('create')" />
        <n-input-number v-model:value="createForm.supplierId" :show-button="false" placeholder="供应商ID（可选）" />
        <n-select v-model:value="createForm.productId" :options="productOptions" placeholder="选择商品" />
        <n-select v-model:value="createForm.locationId" :options="createLocationOptions" placeholder="选择库位" />
        <n-input-number v-model:value="createForm.qty" :show-button="false" placeholder="数量" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button @click="createVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="onCreate">保存草稿</n-button>
        </div>
      </template>
    </n-modal>

    <n-modal v-model:show="detailVisible" preset="card" title="入库单详情" class="max-w-6xl">
      <div class="space-y-3">
        <n-descriptions bordered :column="3" size="small" label-placement="left" v-if="detail.order">
          <n-descriptions-item label="单号">{{ detail.order.inboundNo }}</n-descriptions-item>
          <n-descriptions-item label="状态">{{ statusText(detail.order.status) }}</n-descriptions-item>
          <n-descriptions-item label="下一步">{{ nextStep(detail.order.status) }}</n-descriptions-item>
        </n-descriptions>
        <article class="rounded-xl border border-border bg-bg/60 p-3">
          <div class="mb-2 flex items-center justify-between">
            <p class="text-sm font-medium">处理历史时间线</p>
            <n-button class="nd-soft-focus" size="small" :disabled="!detail.auditQuery?.bizNo" @click="openAudit(detail.auditQuery)">
              查看审计记录
            </n-button>
          </div>
          <n-data-table :columns="timelineColumns" :data="detail.timeline" :bordered="false" size="small" />
        </article>
        <article class="rounded-xl border border-border bg-bg/60 p-3">
          <p class="mb-2 text-sm font-medium">入库明细</p>
          <n-data-table :columns="itemColumns" :data="detail.items" :bordered="false" size="small" />
        </article>
      </div>
    </n-modal>
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { NButton, NDataTable, NDescriptions, NDescriptionsItem, NInputNumber, NModal, NSelect, NTag, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { wmsApi, type InboundOrder, type InboundOrderItem, type Location, type OrderDetail, type OrderTimelineItem, type Product, type Warehouse } from "@/services/wms";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
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
const createLocations = ref<Location[]>([]);
const createForm = reactive({
  warehouseId: null as string | null,
  supplierId: 0,
  productId: null as string | null,
  locationId: null as string | null,
  qty: 1
});

const warehouseOptions = computed(() => warehouses.value.map((v) => ({ label: `${v.warehouseCode} - ${v.warehouseName}`, value: v.id })));
const productOptions = computed(() => products.value.map((v) => ({ label: `${v.productCode} - ${v.productName}`, value: v.id })));
const createLocationOptions = computed(() => createLocations.value.map((v) => ({ label: `${v.locationCode} - ${v.locationName}`, value: v.id })));

const itemColumns: DataTableColumns<InboundOrderItem> = [
  { title: "行号", key: "lineNo", width: 80 },
  { title: "商品ID", key: "productId" },
  { title: "库位ID", key: "locationId" },
  { title: "计划数量", key: "planQty" },
  { title: "实收数量", key: "receivedQty" },
  { title: "合格数量", key: "qualifiedQty" }
];

const timelineColumns: DataTableColumns<OrderTimelineItem> = [
  { title: "操作时间", key: "occurredAt", width: 180 },
  { title: "操作人", key: "operatorName", width: 120 },
  { title: "动作", key: "actionLabel", width: 140 },
  { title: "状态变化", key: "status", width: 170, render: (row) => `${row.statusFrom || "-"} -> ${row.statusTo || "-"}` },
  { title: "备注/意见", key: "note" }
];

const columns: DataTableColumns<InboundOrder> = [
  { title: "单号", key: "inboundNo", minWidth: 180 },
  { title: "状态", key: "status", width: 120, render: (row) => h(NTag, { type: statusType(row.status), bordered: false }, { default: () => statusText(row.status) }) },
  { title: "下一步", key: "next", minWidth: 220, render: (row) => nextStep(row.status) },
  {
    title: "操作",
    key: "actions",
    width: 620,
    render: (row) =>
      h("div", { class: "flex flex-wrap gap-2" }, [
        actionBtn("详情", () => openDetail(row.id), true, row.id),
        actionBtn("提交", () => runAction("submit", row), (row.status === "DRAFT" || row.status === "REJECTED") && authStore.hasPermission("INBOUND_SUBMIT"), row.id),
        actionBtn("撤回", () => runAction("withdraw", row), row.status === "SUBMITTED" && authStore.hasPermission("INBOUND_WITHDRAW"), row.id),
        actionBtn("审核通过", () => runAction("approve", row), row.status === "SUBMITTED" && authStore.hasPermission("INBOUND_APPROVE"), row.id),
        actionBtn("驳回", () => runAction("reject", row), row.status === "SUBMITTED" && authStore.hasPermission("INBOUND_APPROVE"), row.id),
        actionBtn("反审核", () => runAction("unapprove", row), row.status === "APPROVED" && authStore.hasPermission("INBOUND_UNAPPROVE"), row.id),
        actionBtn("作废", () => runAction("cancel", row), (row.status === "DRAFT" || row.status === "SUBMITTED" || row.status === "REJECTED") && authStore.hasPermission("INBOUND_CANCEL"), row.id),
        actionBtn("过账", () => runAction("post", row), row.status === "APPROVED" && authStore.hasPermission("INBOUND_POST"), row.id, true)
      ])
  }
];

function actionBtn(label: string, onClick: () => void, enabled: boolean, rowId: string, primary = false) {
  return h(NButton, {
    size: "small",
    type: primary ? "primary" : "default",
    disabled: !enabled || Boolean(actionLoading.value),
    loading: actionLoading.value === `${rowId}:${label}`,
    onClick
  }, { default: () => label });
}

function statusType(status: string) {
  if (status === "POSTED") return "success";
  if (status === "APPROVED") return "info";
  if (status === "REJECTED" || status === "CANCELED") return "error";
  return "warning";
}

function statusText(status: string) {
  const map: Record<string, string> = {
    DRAFT: "草稿",
    SUBMITTED: "待审核",
    APPROVED: "已审核",
    REJECTED: "已驳回",
    CANCELED: "已作废",
    POSTED: "已过账"
  };
  return map[status] || status;
}

function nextStep(status: string) {
  const map: Record<string, string> = {
    DRAFT: "补充明细后提交审核",
    SUBMITTED: "等待管理员审核或驳回",
    APPROVED: "执行过账入库",
    REJECTED: "根据驳回原因修正后重提",
    CANCELED: "单据已结束，可重新建单",
    POSTED: "流程完成，可到库存页复核"
  };
  return map[status] || "按当前状态继续处理";
}

async function onWarehouseChange(scope: "create") {
  if (scope === "create") {
    createLocations.value = createForm.warehouseId ? await wmsApi.listLocations(createForm.warehouseId) : [];
    createForm.locationId = createLocations.value[0]?.id || null;
  }
}

async function loadMaster() {
  const [w, p] = await Promise.all([wmsApi.listWarehouses(), wmsApi.listProducts()]);
  warehouses.value = w;
  products.value = p;
  if (!createForm.warehouseId && w.length > 0) createForm.warehouseId = w[0].id;
  if (!createForm.productId && p.length > 0) createForm.productId = p[0].id;
  await onWarehouseChange("create");
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
    ui.warning("请先补全仓库、商品、库位与数量");
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
  try {
    detail.value = await wmsApi.getInboundOrderDetail(id, { force: true });
    detailVisible.value = true;
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "详情加载失败";
  }
}

function askActionNote(action: string) {
  const title: Record<string, string> = {
    submit: "提交备注",
    approve: "审核意见",
    reject: "驳回原因",
    withdraw: "撤回原因",
    cancel: "作废原因",
    unapprove: "反审核说明",
    post: "过账备注"
  };
  return window.prompt(`请输入${title[action] || "处理备注"}（可留空）：`) || "";
}

async function runAction(action: "submit" | "withdraw" | "approve" | "reject" | "unapprove" | "cancel" | "post", row: InboundOrder) {
  if ((action === "approve" || action === "reject" || action === "unapprove") && !authStore.hasPermission("INBOUND_APPROVE") && !authStore.hasPermission("INBOUND_UNAPPROVE")) {
    ui.warning("只有管理员可以执行审核类操作");
    return;
  }
  if ((action === "approve" || action === "reject") && String(row.createdBy || "") === String(authStore.profile?.userId || "")) {
    ui.warning("创建人和审核人必须分离，请由其他管理员处理");
    return;
  }
  const note = askActionNote(action);
  actionLoading.value = `${row.id}:${action}`;
  try {
    if (action === "submit") await wmsApi.submitInboundOrder(row.id, note);
    if (action === "withdraw") await wmsApi.withdrawInboundOrder(row.id, note);
    if (action === "approve") await wmsApi.approveInboundOrder(row.id, note);
    if (action === "reject") await wmsApi.rejectInboundOrder(row.id, note);
    if (action === "unapprove") await wmsApi.unapproveInboundOrder(row.id, note);
    if (action === "cancel") await wmsApi.cancelInboundOrder(row.id, note);
    if (action === "post") await wmsApi.postInboundOrder(row.id, note);
    successText.value = "操作成功";
    await loadList();
    if (detailVisible.value && detail.value.order?.id === row.id) await openDetail(row.id);
  } catch (e) {
    const msg = e instanceof Error ? e.message : "操作失败";
    errorText.value = msg.includes("FORBIDDEN") ? "当前账号无权限执行该业务动作，请联系管理员授权。" : msg;
  } finally {
    actionLoading.value = "";
  }
}

function openAudit(query?: { bizNo?: string; resourceType?: string; resourceId?: string }) {
  router.push({ path: "/system/audit-center", query: { bizNo: query?.bizNo, resourceType: query?.resourceType, resourceId: query?.resourceId } });
}

onMounted(async () => {
  await Promise.all([loadMaster(), loadList()]);
});
</script>
