<template>
  <section class="space-y-4">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">库位管理</h1>
        <p class="text-sm text-text-secondary">支持按仓库筛选、创建库位。</p>
      </div>
      <div class="flex items-center gap-2">
        <n-select
          v-model:value="selectedWarehouseId"
          :options="warehouseOptions"
          clearable
          placeholder="按仓库筛选"
          class="w-56"
          @update:value="loadList"
        />
        <n-button type="primary" @click="createVisible = true">新增库位</n-button>
      </div>
    </header>

    <article class="rounded-2xl border border-border bg-surface p-4 shadow-card">
      <n-data-table :columns="columns" :data="rows" :loading="loading" :bordered="false" />
    </article>

    <n-modal v-model:show="createVisible" preset="card" title="新增库位" class="max-w-lg">
      <div class="space-y-3">
        <n-select v-model:value="form.warehouseId" :options="warehouseOptions" placeholder="仓库" />
        <n-input v-model:value="form.locationCode" placeholder="库位编码" />
        <n-input v-model:value="form.locationName" placeholder="库位名称" />
        <div class="grid grid-cols-2 gap-3">
          <n-input v-model:value="form.locationType" placeholder="库位类型" />
          <n-input-number v-model:value="form.capacityQty" :show-button="false" placeholder="容量" />
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
import { computed, onMounted, reactive, ref } from "vue";
import { NButton, NDataTable, NInput, NInputNumber, NModal, NSelect, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { wmsApi, type Location, type Warehouse } from "@/services/wms";

const message = useMessage();
const loading = ref(false);
const submitting = ref(false);
const createVisible = ref(false);
const selectedWarehouseId = ref<number | null>(null);
const rows = ref<Location[]>([]);
const warehouses = ref<Warehouse[]>([]);

const form = reactive({
  warehouseId: null as number | null,
  locationCode: "",
  locationName: "",
  locationType: "STORAGE",
  capacityQty: 100
});

const warehouseOptions = computed(() => warehouses.value.map((item) => ({ label: `${item.warehouseCode} - ${item.warehouseName}`, value: item.id })));

const columns: DataTableColumns<Location> = [
  { title: "ID", key: "id", width: 80 },
  { title: "仓库ID", key: "warehouseId" },
  { title: "库位编码", key: "locationCode" },
  { title: "库位名称", key: "locationName" },
  { title: "类型", key: "locationType", render: (row) => row.locationType || "-" },
  { title: "容量", key: "capacityQty", render: (row) => row.capacityQty ?? "-" }
];

async function loadWarehouses() {
  warehouses.value = await wmsApi.listWarehouses();
}

async function loadList() {
  loading.value = true;
  try {
    rows.value = await wmsApi.listLocations(selectedWarehouseId.value ?? undefined);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "库位列表加载失败");
  } finally {
    loading.value = false;
  }
}

async function onCreate() {
  if (!form.warehouseId) {
    message.warning("请先选择仓库");
    return;
  }
  if (!form.locationCode.trim() || !form.locationName.trim()) {
    message.warning("请先输入库位编码和库位名称");
    return;
  }
  submitting.value = true;
  try {
    await wmsApi.createLocation({
      warehouseId: form.warehouseId,
      locationCode: form.locationCode,
      locationName: form.locationName,
      locationType: form.locationType || undefined,
      capacityQty: Number(form.capacityQty)
    });
    message.success("库位创建成功");
    createVisible.value = false;
    form.locationCode = "";
    form.locationName = "";
    form.locationType = "STORAGE";
    form.capacityQty = 100;
    await loadList();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "库位创建失败");
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  try {
    await loadWarehouses();
    if (warehouses.value.length > 0) form.warehouseId = warehouses.value[0].id;
    await loadList();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "初始化失败");
  }
});
</script>
