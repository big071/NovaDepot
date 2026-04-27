<template>
  <section class="space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Location</p>
          <h1 class="nd-page-title">库位管理</h1>
          <p class="nd-page-subtitle">支持仓库维度筛选、建档、详情查看与编辑。</p>
        </div>
        <div class="flex items-center gap-2">
          <n-button class="nd-soft-focus" :loading="loading" @click="loadList">刷新</n-button>
          <n-button v-if="canCreatePermission" class="nd-soft-focus" type="primary" :disabled="warehouseOptions.length === 0" @click="createVisible = true">
            新增库位
          </n-button>
        </div>
      </div>
      <div class="nd-hero-meta">
        <span class="nd-pill">库位记录：{{ rows.length }}</span>
      </div>
    </header>
    <n-alert class="nd-state-alert" type="info" :show-icon="false">
      库位是仓内最小管理单元。常见用途：`PICK` 拣选、`STORAGE` 存储、`STAGING` 暂存、`RETURN` 退货。
    </n-alert>

    <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">{{ errorText }}</n-alert>
    <n-alert v-else-if="lastSuccessText" class="nd-state-alert" type="success" :show-icon="false">{{ lastSuccessText }}</n-alert>

    <section class="nd-toolbar">
      <div class="nd-toolbar-group">
        <n-select
          class="w-60 nd-soft-focus"
          :value="selectedWarehouseId"
          :options="warehouseOptions"
          clearable
          placeholder="按仓库筛选"
          @update:value="onWarehouseChange"
        />
      </div>
    </section>

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h3 class="nd-section-title">库位列表</h3>
          <p class="nd-section-subtitle">共 {{ rows.length }} 条记录</p>
        </div>
      </div>
      <div class="nd-table-body">
        <n-data-table class="nd-table" :columns="columns" :data="rows" :loading="loading" :bordered="false" />
        <n-empty
          v-if="!loading && rows.length === 0"
          class="nd-empty-shell mt-4"
          :description="warehouseOptions.length === 0 ? '暂无仓库，请先创建仓库后再建库位。' : '暂无库位数据，请先创建库位。'"
        >
          <template #extra>
            <n-button
              v-if="warehouseOptions.length > 0 && canCreatePermission"
              class="nd-soft-focus"
              type="primary"
              @click="createVisible = true"
            >
              立即创建库位
            </n-button>
          </template>
        </n-empty>
      </div>
    </article>

    <n-modal v-model:show="createVisible" preset="card" title="新增库位" class="max-w-lg">
      <div class="space-y-3">
        <n-select v-model:value="createForm.warehouseId" :options="warehouseOptions" placeholder="仓库" />
        <n-input v-model:value="createForm.locationCode" placeholder="库位编码" />
        <n-input v-model:value="createForm.locationName" placeholder="库位名称" />
        <div class="grid grid-cols-2 gap-3">
          <n-input v-model:value="createForm.locationType" placeholder="库位类型" />
          <n-input-number v-model:value="createForm.capacityQty" :show-button="false" placeholder="容量" />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" @click="createVisible = false">取消</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="submitting" :disabled="!canCreate" @click="onCreate">保存</n-button>
        </div>
      </template>
    </n-modal>

    <n-modal v-model:show="detailVisible" preset="card" title="库位详情" class="max-w-lg">
      <n-descriptions bordered label-placement="left" :column="1">
        <n-descriptions-item label="ID">{{ detailRow?.id ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="仓库ID">{{ detailRow?.warehouseId ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="库位编码">{{ detailRow?.locationCode ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="库位名称">{{ detailRow?.locationName ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="库位类型">{{ detailRow?.locationType ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="容量">{{ detailRow?.capacityQty ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="状态">{{ detailRow?.status ?? "-" }}</n-descriptions-item>
      </n-descriptions>
    </n-modal>

    <n-modal v-model:show="editVisible" preset="card" title="编辑库位" class="max-w-lg">
      <div class="space-y-3">
        <n-select v-model:value="editForm.warehouseId" :options="warehouseOptions" placeholder="仓库" />
        <n-input v-model:value="editForm.locationCode" placeholder="库位编码" />
        <n-input v-model:value="editForm.locationName" placeholder="库位名称" />
        <div class="grid grid-cols-2 gap-3">
          <n-input v-model:value="editForm.locationType" placeholder="库位类型" />
          <n-input-number v-model:value="editForm.capacityQty" :show-button="false" placeholder="容量" />
        </div>
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
  NSelect,
  useMessage
} from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { wmsApi, type Location, type Warehouse } from "@/services/wms";
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
const selectedWarehouseId = ref<string | null>(null);
const rows = ref<Location[]>([]);
const warehouses = ref<Warehouse[]>([]);
const detailRow = ref<Location | null>(null);
const editingId = ref<string | null>(null);

const createForm = reactive({
  warehouseId: null as string | null,
  locationCode: "",
  locationName: "",
  locationType: "STORAGE",
  capacityQty: 100
});
const editForm = reactive({
  warehouseId: null as string | null,
  locationCode: "",
  locationName: "",
  locationType: "STORAGE",
  capacityQty: 100
});

const warehouseOptions = computed(() => warehouses.value.map((item) => ({ label: `${item.warehouseCode} - ${item.warehouseName}`, value: item.id })));
const canCreatePermission = computed(() => authStore.hasPermission("LOCATION_CREATE"));
const canUpdatePermission = computed(() => authStore.hasPermission("LOCATION_UPDATE"));
const canCreate = computed(
  () => canCreatePermission.value && Boolean(createForm.warehouseId && createForm.locationCode.trim() && createForm.locationName.trim()) && !submitting.value
);
const canEdit = computed(
  () => canUpdatePermission.value && Boolean(editingId.value && editForm.warehouseId && editForm.locationCode.trim() && editForm.locationName.trim()) && !submitting.value
);

const columns: DataTableColumns<Location> = [
  { title: "ID", key: "id", width: 80 },
  { title: "仓库ID", key: "warehouseId" },
  { title: "库位编码", key: "locationCode" },
  { title: "库位名称", key: "locationName" },
  { title: "类型", key: "locationType", render: (row) => row.locationType || "-" },
  { title: "容量", key: "capacityQty", render: (row) => row.capacityQty ?? "-" },
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
            onClick: () => openDetail(row.id)
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

async function loadWarehouses() {
  warehouses.value = await wmsApi.listWarehouses({ force: true });
  if (!createForm.warehouseId && warehouses.value.length > 0) createForm.warehouseId = warehouses.value[0].id;
}

async function loadList() {
  loading.value = true;
  errorText.value = "";
  try {
    rows.value = await wmsApi.listLocations(selectedWarehouseId.value ?? undefined, { force: true });
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "库位列表加载失败";
    message.error(errorText.value);
  } finally {
    loading.value = false;
  }
}

async function onWarehouseChange(value: string | null) {
  selectedWarehouseId.value = value;
  await loadList();
}

async function onCreate() {
  if (!canCreate.value) {
    message.warning("请先填写必填项");
    return;
  }
  submitting.value = true;
  errorText.value = "";
  try {
    await wmsApi.createLocation({
      warehouseId: createForm.warehouseId as string,
      locationCode: createForm.locationCode.trim(),
      locationName: createForm.locationName.trim(),
      locationType: createForm.locationType.trim() || undefined,
      capacityQty: Number(createForm.capacityQty)
    });
    createVisible.value = false;
    createForm.locationCode = "";
    createForm.locationName = "";
    createForm.locationType = "STORAGE";
    createForm.capacityQty = 100;
    await loadList();
    lastSuccessText.value = "库位创建成功";
    message.success(lastSuccessText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "库位创建失败";
    message.error(errorText.value);
  } finally {
    submitting.value = false;
  }
}

async function openDetail(locationId?: string) {
  if (!locationId) return;
  errorText.value = "";
  try {
    detailRow.value = await wmsApi.getLocationDetail(locationId);
    detailVisible.value = true;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "库位详情加载失败";
    message.error(errorText.value);
  }
}

async function openEdit(locationId?: string) {
  if (!locationId) return;
  errorText.value = "";
  try {
    const detail = await wmsApi.getLocationDetail(locationId);
    editingId.value = detail.id;
    editForm.warehouseId = detail.warehouseId;
    editForm.locationCode = detail.locationCode;
    editForm.locationName = detail.locationName;
    editForm.locationType = detail.locationType || "STORAGE";
    editForm.capacityQty = Number(detail.capacityQty ?? 100);
    editVisible.value = true;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "库位详情加载失败";
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
    await wmsApi.updateLocation(editingId.value, {
      warehouseId: editForm.warehouseId as string,
      locationCode: editForm.locationCode.trim(),
      locationName: editForm.locationName.trim(),
      locationType: editForm.locationType.trim() || undefined,
      capacityQty: Number(editForm.capacityQty)
    });
    editVisible.value = false;
    await loadList();
    if (detailVisible.value && detailRow.value?.id === editingId.value) {
      detailRow.value = await wmsApi.getLocationDetail(editingId.value);
    }
    lastSuccessText.value = "库位编辑成功";
    message.success(lastSuccessText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "库位编辑失败";
    message.error(errorText.value);
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  try {
    await loadWarehouses();
    await loadList();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "初始化失败";
    message.error(errorText.value);
  }
});
</script>
