<template>
  <section class="space-y-4">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">入库管理</h1>
        <p class="text-sm text-text-secondary">最小可用：列表 + 新建 + 审核 + 过账。</p>
      </div>
      <n-button type="primary" @click="createVisible = true">新建入库单</n-button>
    </header>

    <article class="rounded-2xl border border-border bg-surface p-4 shadow-card">
      <n-data-table :columns="columns" :data="rows" :loading="loading" :bordered="false" />
    </article>

    <n-modal v-model:show="createVisible" preset="card" title="新建入库单" class="max-w-xl">
      <div class="space-y-3">
        <div class="grid grid-cols-2 gap-3">
          <n-input-number v-model:value="form.warehouseId" :show-button="false" placeholder="仓库ID" />
          <n-input-number v-model:value="form.supplierId" :show-button="false" placeholder="供应商ID（可选）" />
        </div>
        <div class="grid grid-cols-3 gap-3">
          <n-input-number v-model:value="form.productId" :show-button="false" placeholder="商品ID" />
          <n-input-number v-model:value="form.locationId" :show-button="false" placeholder="库位ID" />
          <n-input-number v-model:value="form.qty" :show-button="false" placeholder="数量" />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button @click="createVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="onCreate">保存</n-button>
        </div>
      </template>
    </n-modal>
  </section>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from "vue";
import { NButton, NDataTable, NInputNumber, NModal, NTag, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { wmsApi, type InboundOrder } from "@/services/wms";

const message = useMessage();
const loading = ref(false);
const submitting = ref(false);
const actionLoading = ref<number | null>(null);
const createVisible = ref(false);
const rows = ref<InboundOrder[]>([]);

const form = reactive({ warehouseId: 1, supplierId: 0, productId: 1, locationId: 1, qty: 1 });

const columns: DataTableColumns<InboundOrder> = [
  { title: "ID", key: "id", width: 80 },
  { title: "入库单号", key: "inboundNo" },
  { title: "仓库ID", key: "warehouseId" },
  { title: "状态", key: "status", render: (row) => h(NTag, { type: row.status === "POSTED" ? "success" : "warning" }, { default: () => row.status }) },
  {
    title: "操作",
    key: "actions",
    render: (row) => h("div", { class: "flex gap-2" }, [
      h(NButton, { size: "small", loading: actionLoading.value === row.id, onClick: () => runAction("approve", row.id) }, { default: () => "审核" }),
      h(NButton, { size: "small", type: "primary", loading: actionLoading.value === row.id, onClick: () => runAction("post", row.id) }, { default: () => "过账" })
    ])
  }
];

async function loadList() {
  loading.value = true;
  try {
    rows.value = await wmsApi.listInboundOrders();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "入库单加载失败");
  } finally {
    loading.value = false;
  }
}

async function onCreate() {
  if (!form.warehouseId || !form.productId || !form.locationId || !form.qty || Number(form.qty) <= 0) {
    message.warning("请填写完整且有效的入库单信息");
    return;
  }
  submitting.value = true;
  try {
    await wmsApi.createInboundOrder({
      warehouseId: Number(form.warehouseId),
      supplierId: form.supplierId ? Number(form.supplierId) : undefined,
      items: [{ productId: Number(form.productId), locationId: Number(form.locationId), qty: Number(form.qty) }]
    });
    message.success("入库单创建成功");
    createVisible.value = false;
    await loadList();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "入库单创建失败");
  } finally {
    submitting.value = false;
  }
}

async function runAction(action: "approve" | "post", id: number) {
  actionLoading.value = id;
  try {
    if (action === "approve") {
      await wmsApi.approveInboundOrder(id);
      message.success("入库单审核成功");
    } else {
      await wmsApi.postInboundOrder(id);
      message.success("入库单过账成功");
    }
    await loadList();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "操作失败");
  } finally {
    actionLoading.value = null;
  }
}

onMounted(loadList);
</script>
