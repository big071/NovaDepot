<template>
  <section class="space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Master Data</p>
          <h1 class="nd-page-title">商品管理</h1>
          <p class="nd-page-subtitle">统一维护商品主数据，支持新增、详情查看和编辑修正。</p>
        </div>
        <div class="flex items-center gap-2">
          <n-button class="nd-soft-focus" :loading="loading" @click="loadList">刷新</n-button>
          <n-button v-if="canCreatePermission" class="nd-soft-focus" type="primary" @click="createVisible = true">新增商品</n-button>
        </div>
      </div>
      <div class="nd-hero-meta">
        <span class="nd-pill">主数据记录：{{ rows.length }}</span>
      </div>
    </header>
    <n-alert class="nd-state-alert" type="info" :show-icon="false">
      商品是入库、出库、库存、客服和 AI 分析的基础对象。建议先建立商品，再进行单据流转。
    </n-alert>

    <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">
      <div class="flex items-center justify-between gap-2">
        <span>{{ errorText }}</span>
        <n-button text type="primary" @click="loadList">重试</n-button>
      </div>
    </n-alert>
    <n-alert v-else-if="lastSuccessText" class="nd-state-alert" type="success" :show-icon="false">{{ lastSuccessText }}</n-alert>

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h3 class="nd-section-title">商品列表</h3>
          <p class="nd-section-subtitle">共 {{ rows.length }} 条记录</p>
        </div>
      </div>
      <div class="nd-table-body">
        <n-data-table class="nd-table" :columns="columns" :data="rows" :loading="loading" :bordered="false" />
        <n-empty v-if="!loading && rows.length === 0" class="nd-empty-shell mt-4" description="暂无商品数据，请先创建商品主数据。">
          <template #extra>
            <n-button v-if="canCreatePermission" class="nd-soft-focus" type="primary" @click="createVisible = true">立即创建商品</n-button>
          </template>
        </n-empty>
      </div>
    </article>

    <n-modal v-model:show="createVisible" preset="card" title="新增商品" class="max-w-lg">
      <div class="space-y-3">
        <n-input v-model:value="createForm.productCode" placeholder="商品编码" />
        <n-input v-model:value="createForm.productName" placeholder="商品名称" />
        <div class="grid grid-cols-2 gap-3">
          <n-input-number v-model:value="createForm.categoryId" :show-button="false" placeholder="分类ID" />
          <n-input-number v-model:value="createForm.unitId" :show-button="false" placeholder="单位ID" />
        </div>
        <n-input v-model:value="createForm.barcode" placeholder="条码（可选）" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" @click="createVisible = false">取消</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="submitting" :disabled="!canCreate" @click="onCreate">保存</n-button>
        </div>
      </template>
    </n-modal>

    <n-modal v-model:show="detailVisible" preset="card" title="商品详情" class="max-w-lg">
      <n-descriptions bordered label-placement="left" :column="1">
        <n-descriptions-item label="ID">{{ detailRow?.id ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="商品编码">{{ detailRow?.productCode ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="商品名称">{{ detailRow?.productName ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="条码">{{ detailRow?.barcode ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="状态">{{ detailRow?.status ?? "-" }}</n-descriptions-item>
      </n-descriptions>
    </n-modal>

    <n-modal v-model:show="editVisible" preset="card" title="编辑商品" class="max-w-lg">
      <div class="space-y-3">
        <n-input v-model:value="editForm.productCode" placeholder="商品编码" />
        <n-input v-model:value="editForm.productName" placeholder="商品名称" />
        <div class="grid grid-cols-2 gap-3">
          <n-input-number v-model:value="editForm.categoryId" :show-button="false" placeholder="分类ID" />
          <n-input-number v-model:value="editForm.unitId" :show-button="false" placeholder="单位ID" />
        </div>
        <n-input v-model:value="editForm.barcode" placeholder="条码（可选）" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" @click="editVisible = false">取消</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="submitting" :disabled="!canEdit" @click="onUpdate">保存修改</n-button>
        </div>
      </template>
    </n-modal>
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from "vue";
import {
  NAlert,
  NButton,
  NDataTable,
  NDescriptions,
  NDescriptionsItem,
  NEmpty,
  NInput,
  NInputNumber,
  NModal,
  useMessage
} from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { wmsApi, type Product } from "@/services/wms";
import { useAuthStore } from "@/stores/auth";

const message = useMessage();
const authStore = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const errorText = ref("");
const lastSuccessText = ref("");
const createVisible = ref(false);
const detailVisible = ref(false);
const editVisible = ref(false);
const rows = ref<Product[]>([]);
const detailRow = ref<Product | null>(null);
const editingId = ref<string | null>(null);

const createForm = reactive({ productCode: "", productName: "", categoryId: 1, unitId: 1, barcode: "" });
const editForm = reactive({ productCode: "", productName: "", categoryId: 1, unitId: 1, barcode: "" });
const canCreatePermission = computed(() => authStore.hasPermission("PRODUCT_CREATE"));
const canUpdatePermission = computed(() => authStore.hasPermission("PRODUCT_UPDATE"));

const canCreate = computed(() => canCreatePermission.value && Boolean(createForm.productCode.trim() && createForm.productName.trim()) && !submitting.value);
const canEdit = computed(() => canUpdatePermission.value && Boolean(editForm.productCode.trim() && editForm.productName.trim() && editingId.value) && !submitting.value);

const columns: DataTableColumns<Product> = [
  { title: "ID", key: "id", width: 80 },
  { title: "商品编码", key: "productCode" },
  { title: "商品名称", key: "productName" },
  { title: "条码", key: "barcode", render: (row) => row.barcode || "-" },
  { title: "状态", key: "status", render: (row) => row.status || "ENABLED" },
  {
    title: "操作",
    key: "actions",
    width: 180,
    render: (row) =>
      h("div", { class: "flex gap-2" }, [
        h(
          NButton,
          {
            class: "nd-soft-focus",
            size: "small",
            onClick: () => openDetail(row.productCode)
          },
          { default: () => "详情" }
        ),
        canUpdatePermission.value
          ? h(
            NButton,
            {
              class: "nd-soft-focus",
              size: "small",
              type: "primary",
              onClick: () => openEdit(row.id)
            },
            { default: () => "编辑" }
          )
          : null
      ])
  }
];

async function loadList() {
  loading.value = true;
  errorText.value = "";
  try {
    rows.value = await wmsApi.listProducts();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "商品列表加载失败";
    message.error(errorText.value);
  } finally {
    loading.value = false;
  }
}

async function onCreate() {
  if (!canCreate.value) {
    message.warning("请先填写必填项");
    return;
  }
  submitting.value = true;
  errorText.value = "";
  try {
    await wmsApi.createProduct({
      productCode: createForm.productCode.trim(),
      productName: createForm.productName.trim(),
      categoryId: Number(createForm.categoryId),
      unitId: Number(createForm.unitId),
      barcode: createForm.barcode.trim() || undefined
    });
    createVisible.value = false;
    createForm.productCode = "";
    createForm.productName = "";
    createForm.categoryId = 1;
    createForm.unitId = 1;
    createForm.barcode = "";
    await loadList();
    lastSuccessText.value = "商品创建成功";
    message.success(lastSuccessText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "商品创建失败";
    message.error(errorText.value);
  } finally {
    submitting.value = false;
  }
}

async function openDetail(productCode?: string) {
  if (!productCode) return;
  errorText.value = "";
  try {
    detailRow.value = await wmsApi.getProductDetailByCode(productCode);
    detailVisible.value = true;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "商品详情加载失败";
    message.error(errorText.value);
  }
}

async function openEdit(id?: string) {
  if (!id) return;
  errorText.value = "";
  try {
    const detail = await wmsApi.getProductDetail(id);
    editingId.value = detail.id;
    editForm.productCode = detail.productCode;
    editForm.productName = detail.productName;
    editForm.categoryId = 1;
    editForm.unitId = 1;
    editForm.barcode = detail.barcode || "";
    editVisible.value = true;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "商品详情加载失败";
    message.error(errorText.value);
  }
}

async function onUpdate() {
  if (!editingId.value || !canEdit.value) {
    message.warning("请先填写必填项");
    return;
  }
  submitting.value = true;
  errorText.value = "";
  try {
    await wmsApi.updateProduct(editingId.value, {
      productCode: editForm.productCode.trim(),
      productName: editForm.productName.trim(),
      categoryId: Number(editForm.categoryId),
      unitId: Number(editForm.unitId),
      barcode: editForm.barcode.trim() || undefined
    });
    editVisible.value = false;
    await loadList();
    if (detailVisible.value && detailRow.value?.id === editingId.value) {
      detailRow.value = await wmsApi.getProductDetail(editingId.value);
    }
    lastSuccessText.value = "商品编辑成功";
    message.success(lastSuccessText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "商品编辑失败";
    message.error(errorText.value);
  } finally {
    submitting.value = false;
  }
}

onMounted(loadList);
</script>
