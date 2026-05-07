<template>
  <section class="space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Master Data</p>
          <h1 class="nd-page-title">鍟嗗搧绠＄悊</h1>
          <p class="nd-page-subtitle">Maintain product master data and CSV imports.</p>
        </div>
        <div class="flex items-center gap-2">
          <input ref="importInput" class="hidden" type="file" accept=".csv,text/csv" @change="onImportFile" />
          <n-button v-if="authStore.hasPermission('PRODUCT_TEMPLATE_EXPORT')" class="nd-soft-focus" @click="downloadTemplate">CSV模板</n-button>
          <n-button v-if="authStore.hasPermission('PRODUCT_IMPORT')" class="nd-soft-focus" @click="importInput?.click()">CSV导入</n-button>
          <n-button class="nd-soft-focus" :loading="loading" @click="loadList">刷新</n-button>
          <n-button v-if="canCreatePermission" class="nd-soft-focus" type="primary" @click="createVisible = true">鏂板鍟嗗搧</n-button>
        </div>
      </div>
      <div class="nd-hero-meta">
        <span class="nd-pill">涓绘暟鎹褰曪細{{ rows.length }}</span>
      </div>
    </header>
    <n-alert class="nd-state-alert" type="info" :show-icon="false">
      鍟嗗搧鏄叆搴撱€佸嚭搴撱€佸簱瀛樸€佸鏈嶅拰 AI 鍒嗘瀽鐨勫熀纭€瀵硅薄銆傚缓璁厛寤虹珛鍟嗗搧锛屽啀杩涜鍗曟嵁娴佽浆銆?
    </n-alert>

    <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">
      <div class="flex items-center justify-between gap-2">
        <span>{{ errorText }}</span>
        <n-button text type="primary" @click="loadList">閲嶈瘯</n-button>
      </div>
    </n-alert>
    <n-alert v-else-if="lastSuccessText" class="nd-state-alert" type="success" :show-icon="false">{{ lastSuccessText }}</n-alert>

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h3 class="nd-section-title">鍟嗗搧鍒楄〃</h3>
          <p class="nd-section-subtitle">{{ rows.length }} records</p>
        </div>
      </div>
      <div class="nd-table-body">
        <n-data-table class="nd-table" :columns="columns" :data="rows" :loading="loading" :bordered="false" />
        <n-empty v-if="!loading && rows.length === 0" class="nd-empty-shell mt-4" description="No product data yet.">
          <template #extra>
            <n-button v-if="canCreatePermission" class="nd-soft-focus" type="primary" @click="createVisible = true">Create Product</n-button>
          </template>
        </n-empty>
      </div>
    </article>

    <n-modal v-model:show="createVisible" preset="card" title="鏂板鍟嗗搧" class="max-w-lg">
      <div class="space-y-3">
        <n-input v-model:value="createForm.productCode" placeholder="鍟嗗搧缂栫爜" />
        <n-input v-model:value="createForm.productName" placeholder="鍟嗗搧鍚嶇О" />
        <div class="grid grid-cols-2 gap-3">
          <n-input-number v-model:value="createForm.categoryId" :show-button="false" placeholder="鍒嗙被ID" />
          <n-input-number v-model:value="createForm.unitId" :show-button="false" placeholder="鍗曚綅ID" />
        </div>
        <n-input v-model:value="createForm.barcode" placeholder="鏉＄爜锛堝彲閫夛級" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" @click="createVisible = false">鍙栨秷</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="submitting" :disabled="!canCreate" @click="onCreate">淇濆瓨</n-button>
        </div>
      </template>
    </n-modal>

    <n-modal v-model:show="detailVisible" preset="card" title="鍟嗗搧璇︽儏" class="max-w-lg">
      <n-descriptions bordered label-placement="left" :column="1">
        <n-descriptions-item label="ID">{{ detailRow?.id ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="鍟嗗搧缂栫爜">{{ detailRow?.productCode ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="鍟嗗搧鍚嶇О">{{ detailRow?.productName ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="鏉＄爜">{{ detailRow?.barcode ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="Status">{{ detailRow?.status ?? "-" }}</n-descriptions-item>
      </n-descriptions>
    </n-modal>

    <n-modal v-model:show="editVisible" preset="card" title="缂栬緫鍟嗗搧" class="max-w-lg">
      <div class="space-y-3">
        <n-input v-model:value="editForm.productCode" placeholder="鍟嗗搧缂栫爜" />
        <n-input v-model:value="editForm.productName" placeholder="鍟嗗搧鍚嶇О" />
        <div class="grid grid-cols-2 gap-3">
          <n-input-number v-model:value="editForm.categoryId" :show-button="false" placeholder="鍒嗙被ID" />
          <n-input-number v-model:value="editForm.unitId" :show-button="false" placeholder="鍗曚綅ID" />
        </div>
        <n-input v-model:value="editForm.barcode" placeholder="鏉＄爜锛堝彲閫夛級" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" @click="editVisible = false">鍙栨秷</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="submitting" :disabled="!canEdit" @click="onUpdate">淇濆瓨淇敼</n-button>
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
const importInput = ref<HTMLInputElement | null>(null);

const createForm = reactive({ productCode: "", productName: "", categoryId: 1, unitId: 1, barcode: "" });
const editForm = reactive({ productCode: "", productName: "", categoryId: 1, unitId: 1, barcode: "" });
const canCreatePermission = computed(() => authStore.hasPermission("PRODUCT_CREATE"));
const canUpdatePermission = computed(() => authStore.hasPermission("PRODUCT_UPDATE"));

const canCreate = computed(() => canCreatePermission.value && Boolean(createForm.productCode.trim() && createForm.productName.trim()) && !submitting.value);
const canEdit = computed(() => canUpdatePermission.value && Boolean(editForm.productCode.trim() && editForm.productName.trim() && editingId.value) && !submitting.value);

const columns: DataTableColumns<Product> = [
  { title: "ID", key: "id", width: 80 },
  { title: "鍟嗗搧缂栫爜", key: "productCode" },
  { title: "鍟嗗搧鍚嶇О", key: "productName" },
  { title: "鏉＄爜", key: "barcode", render: (row) => row.barcode || "-" },
  { title: "Status", key: "status", render: (row) => row.status || "ENABLED" },
  {
    title: "鎿嶄綔",
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
          { default: () => "璇︽儏" }
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
            { default: () => "缂栬緫" }
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
    errorText.value = error instanceof Error ? error.message : "鍟嗗搧鍒楄〃鍔犺浇澶辫触";
    message.error(errorText.value);
  } finally {
    loading.value = false;
  }
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
    downloadText("products-import-template.csv", await wmsApi.exportProductImportTemplate());
  } catch (error) {
    message.error(error instanceof Error ? error.message : "模板下载失败");
  }
}

async function onImportFile(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;
  try {
    const result = await wmsApi.importProducts(await file.text());
    lastSuccessText.value = `CSV导入完成：成功 ${result.successRows}，失败 ${result.failedRows}，跳过 ${result.skippedRows}`;
    if (result.reportId) {
      downloadText(`product-import-errors-${result.reportId}.csv`, await wmsApi.getProductImportErrorReport(result.reportId));
    }
    await loadList();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "CSV导入失败");
  } finally {
    if (importInput.value) importInput.value.value = "";
  }
}
async function onCreate() {
  if (!canCreate.value) {
    message.warning("Please complete required fields");
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
    lastSuccessText.value = "鍟嗗搧鍒涘缓鎴愬姛";
    message.success(lastSuccessText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "鍟嗗搧鍒涘缓澶辫触";
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
    errorText.value = error instanceof Error ? error.message : "鍟嗗搧璇︽儏鍔犺浇澶辫触";
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
    errorText.value = error instanceof Error ? error.message : "鍟嗗搧璇︽儏鍔犺浇澶辫触";
    message.error(errorText.value);
  }
}

async function onUpdate() {
  if (!editingId.value || !canEdit.value) {
    message.warning("Please complete required fields");
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
    lastSuccessText.value = "鍟嗗搧缂栬緫鎴愬姛";
    message.success(lastSuccessText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "鍟嗗搧缂栬緫澶辫触";
    message.error(errorText.value);
  } finally {
    submitting.value = false;
  }
}

onMounted(loadList);
</script>
