<template>
  <section class="space-y-4">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">商品管理</h1>
        <p class="text-sm text-text-secondary">最小可用版本：列表 + 新建商品。</p>
      </div>
      <n-button type="primary" @click="createVisible = true">新增商品</n-button>
    </header>

    <article class="rounded-2xl border border-border bg-surface p-4 shadow-card">
      <n-data-table :columns="columns" :data="rows" :loading="loading" :bordered="false" />
    </article>

    <n-modal v-model:show="createVisible" preset="card" title="新增商品" class="max-w-lg">
      <div class="space-y-3">
        <n-input v-model:value="form.productCode" placeholder="商品编码" />
        <n-input v-model:value="form.productName" placeholder="商品名称" />
        <div class="grid grid-cols-2 gap-3">
          <n-input-number v-model:value="form.categoryId" :show-button="false" placeholder="分类ID" />
          <n-input-number v-model:value="form.unitId" :show-button="false" placeholder="单位ID" />
        </div>
        <n-input v-model:value="form.barcode" placeholder="条码（可选）" />
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
import { onMounted, reactive, ref } from "vue";
import { NButton, NDataTable, NInput, NInputNumber, NModal, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { wmsApi, type Product } from "@/services/wms";

const message = useMessage();
const loading = ref(false);
const submitting = ref(false);
const createVisible = ref(false);
const rows = ref<Product[]>([]);

const form = reactive({ productCode: "", productName: "", categoryId: 1, unitId: 1, barcode: "" });

const columns: DataTableColumns<Product> = [
  { title: "ID", key: "id", width: 80 },
  { title: "商品编码", key: "productCode" },
  { title: "商品名称", key: "productName" },
  { title: "条码", key: "barcode", render: (row) => row.barcode || "-" },
  { title: "状态", key: "status", render: (row) => row.status || "ENABLED" }
];

async function loadList() {
  loading.value = true;
  try {
    rows.value = await wmsApi.listProducts();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "商品列表加载失败");
  } finally {
    loading.value = false;
  }
}

async function onCreate() {
  if (!form.productCode.trim() || !form.productName.trim()) {
    message.warning("请先输入商品编码和商品名称");
    return;
  }
  submitting.value = true;
  try {
    await wmsApi.createProduct({
      productCode: form.productCode,
      productName: form.productName,
      categoryId: Number(form.categoryId),
      unitId: Number(form.unitId),
      barcode: form.barcode || undefined
    });
    message.success("商品创建成功");
    createVisible.value = false;
    form.productCode = "";
    form.productName = "";
    form.categoryId = 1;
    form.unitId = 1;
    form.barcode = "";
    await loadList();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "商品创建失败");
  } finally {
    submitting.value = false;
  }
}

onMounted(loadList);
</script>
