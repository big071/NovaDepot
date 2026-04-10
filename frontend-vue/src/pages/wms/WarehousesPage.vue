<template>
  <section class="space-y-4">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">仓库管理</h1>
        <p class="text-sm text-text-secondary">最小可用版本：列表 + 新建仓库。</p>
      </div>
      <n-button type="primary" @click="createVisible = true">新增仓库</n-button>
    </header>

    <article class="rounded-2xl border border-border bg-surface p-4 shadow-card">
      <n-data-table :columns="columns" :data="rows" :loading="loading" :bordered="false" />
    </article>

    <n-modal v-model:show="createVisible" preset="card" title="新增仓库" class="max-w-lg">
      <div class="space-y-3">
        <n-input v-model:value="form.warehouseCode" placeholder="仓库编码" />
        <n-input v-model:value="form.warehouseName" placeholder="仓库名称" />
        <n-input v-model:value="form.warehouseType" placeholder="仓库类型（可选）" />
        <n-input v-model:value="form.address" placeholder="仓库地址（可选）" />
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
import { NButton, NDataTable, NInput, NModal, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { wmsApi, type Warehouse } from "@/services/wms";

const message = useMessage();
const loading = ref(false);
const submitting = ref(false);
const createVisible = ref(false);
const rows = ref<Warehouse[]>([]);

const form = reactive({ warehouseCode: "", warehouseName: "", warehouseType: "NORMAL", address: "" });

const columns: DataTableColumns<Warehouse> = [
  { title: "ID", key: "id", width: 80 },
  { title: "仓库编码", key: "warehouseCode" },
  { title: "仓库名称", key: "warehouseName" },
  { title: "类型", key: "warehouseType", render: (row) => row.warehouseType || "-" },
  { title: "状态", key: "status", render: (row) => row.status || "ENABLED" }
];

async function loadList() {
  loading.value = true;
  try {
    rows.value = await wmsApi.listWarehouses();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "仓库列表加载失败");
  } finally {
    loading.value = false;
  }
}

async function onCreate() {
  if (!form.warehouseCode.trim() || !form.warehouseName.trim()) {
    message.warning("请先输入仓库编码和仓库名称");
    return;
  }
  submitting.value = true;
  try {
    await wmsApi.createWarehouse({
      warehouseCode: form.warehouseCode,
      warehouseName: form.warehouseName,
      warehouseType: form.warehouseType || undefined,
      address: form.address || undefined
    });
    message.success("仓库创建成功");
    createVisible.value = false;
    form.warehouseCode = "";
    form.warehouseName = "";
    form.warehouseType = "NORMAL";
    form.address = "";
    await loadList();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "仓库创建失败");
  } finally {
    submitting.value = false;
  }
}

onMounted(loadList);
</script>
