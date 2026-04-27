<template>
  <section class="space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Warehouse</p>
          <h1 class="nd-page-title">仓库管理</h1>
          <p class="nd-page-subtitle">仓库主数据维护、查询、详情与编辑。</p>
        </div>
        <div class="flex items-center gap-2">
          <n-button class="nd-soft-focus" :loading="loading" @click="loadList">刷新</n-button>
          <n-button v-if="canCreatePermission" class="nd-soft-focus" type="primary" @click="createVisible = true">新增仓库</n-button>
        </div>
      </div>
      <div class="nd-hero-meta">
        <span class="nd-pill">记录数：{{ rows.length }}</span>
      </div>
    </header>
    <n-alert class="nd-state-alert" type="info" :show-icon="false">
      仓库是库位的上级容器。建议先建仓库，再到“库位管理”按仓库划分拣选位、存储位和暂存位。
    </n-alert>

    <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">{{ errorText }}</n-alert>
    <n-alert v-else-if="lastSuccessText" class="nd-state-alert" type="success" :show-icon="false">{{ lastSuccessText }}</n-alert>

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h3 class="nd-section-title">仓库列表</h3>
          <p class="nd-section-subtitle">共 {{ rows.length }} 条记录</p>
        </div>
      </div>
      <div class="nd-table-body">
        <n-data-table class="nd-table" :columns="columns" :data="rows" :loading="loading" :bordered="false" />
        <n-empty v-if="!loading && rows.length === 0" class="nd-empty-shell mt-4" description="暂无仓库数据，请先创建仓库。">
          <template #extra>
            <n-button v-if="canCreatePermission" class="nd-soft-focus" type="primary" @click="createVisible = true">立即创建仓库</n-button>
          </template>
        </n-empty>
      </div>
    </article>

    <n-modal v-model:show="createVisible" preset="card" title="新增仓库" class="max-w-lg">
      <div class="space-y-3">
        <n-input v-model:value="createForm.warehouseCode" placeholder="仓库编码" />
        <n-input v-model:value="createForm.warehouseName" placeholder="仓库名称" />
        <n-input v-model:value="createForm.warehouseType" placeholder="仓库类型（可选）" />
        <n-input v-model:value="createForm.address" placeholder="仓库地址（可选）" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" @click="createVisible = false">取消</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="submitting" :disabled="!canCreate" @click="onCreate">保存</n-button>
        </div>
      </template>
    </n-modal>

    <n-modal v-model:show="detailVisible" preset="card" title="仓库详情" class="max-w-lg">
      <n-descriptions bordered label-placement="left" :column="1">
        <n-descriptions-item label="ID">{{ detailRow?.id ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="仓库编码">{{ detailRow?.warehouseCode ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="仓库名称">{{ detailRow?.warehouseName ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="仓库类型">{{ detailRow?.warehouseType ?? "-" }}</n-descriptions-item>
        <n-descriptions-item label="状态">{{ detailRow?.status ?? "-" }}</n-descriptions-item>
      </n-descriptions>
    </n-modal>

    <n-modal v-model:show="editVisible" preset="card" title="编辑仓库" class="max-w-lg">
      <div class="space-y-3">
        <n-input v-model:value="editForm.warehouseCode" placeholder="仓库编码" />
        <n-input v-model:value="editForm.warehouseName" placeholder="仓库名称" />
        <n-input v-model:value="editForm.warehouseType" placeholder="仓库类型（可选）" />
        <n-input v-model:value="editForm.address" placeholder="仓库地址（可选）" />
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
  NModal,
  useMessage
} from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { wmsApi, type Warehouse } from "@/services/wms";
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
const rows = ref<Warehouse[]>([]);
const detailRow = ref<Warehouse | null>(null);
const editingId = ref<string | null>(null);

const createForm = reactive({ warehouseCode: "", warehouseName: "", warehouseType: "NORMAL", address: "" });
const editForm = reactive({ warehouseCode: "", warehouseName: "", warehouseType: "NORMAL", address: "" });
const canCreatePermission = computed(() => authStore.hasPermission("WAREHOUSE_CREATE"));
const canUpdatePermission = computed(() => authStore.hasPermission("WAREHOUSE_UPDATE"));
const canCreate = computed(() => canCreatePermission.value && Boolean(createForm.warehouseCode.trim() && createForm.warehouseName.trim()) && !submitting.value);
const canEdit = computed(() => canUpdatePermission.value && Boolean(editForm.warehouseCode.trim() && editForm.warehouseName.trim() && editingId.value) && !submitting.value);

const columns: DataTableColumns<Warehouse> = [
  { title: "ID", key: "id", width: 80 },
  { title: "仓库编码", key: "warehouseCode" },
  { title: "仓库名称", key: "warehouseName" },
  { title: "类型", key: "warehouseType", render: (row) => row.warehouseType || "-" },
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
            onClick: () => openDetail(row.warehouseCode)
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
    rows.value = await wmsApi.listWarehouses({ force: true });
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "仓库列表加载失败";
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
    await wmsApi.createWarehouse({
      warehouseCode: createForm.warehouseCode.trim(),
      warehouseName: createForm.warehouseName.trim(),
      warehouseType: createForm.warehouseType.trim() || undefined,
      address: createForm.address.trim() || undefined
    });
    createVisible.value = false;
    createForm.warehouseCode = "";
    createForm.warehouseName = "";
    createForm.warehouseType = "NORMAL";
    createForm.address = "";
    await loadList();
    lastSuccessText.value = "仓库创建成功";
    message.success(lastSuccessText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "仓库创建失败";
    message.error(errorText.value);
  } finally {
    submitting.value = false;
  }
}

async function openDetail(warehouseCode?: string) {
  if (!warehouseCode) return;
  errorText.value = "";
  try {
    detailRow.value = await wmsApi.getWarehouseDetailByCode(warehouseCode);
    detailVisible.value = true;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "仓库详情加载失败";
    message.error(errorText.value);
  }
}

async function openEdit(id?: string) {
  if (!id) return;
  errorText.value = "";
  try {
    const detail = await wmsApi.getWarehouseDetail(id);
    editingId.value = detail.id;
    editForm.warehouseCode = detail.warehouseCode;
    editForm.warehouseName = detail.warehouseName;
    editForm.warehouseType = detail.warehouseType || "NORMAL";
    editForm.address = "";
    editVisible.value = true;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "仓库详情加载失败";
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
    await wmsApi.updateWarehouse(editingId.value, {
      warehouseCode: editForm.warehouseCode.trim(),
      warehouseName: editForm.warehouseName.trim(),
      warehouseType: editForm.warehouseType.trim() || undefined,
      address: editForm.address.trim() || undefined
    });
    editVisible.value = false;
    await loadList();
    if (detailVisible.value && detailRow.value?.id === editingId.value) {
      detailRow.value = await wmsApi.getWarehouseDetail(editingId.value);
    }
    lastSuccessText.value = "仓库编辑成功";
    message.success(lastSuccessText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "仓库编辑失败";
    message.error(errorText.value);
  } finally {
    submitting.value = false;
  }
}

onMounted(loadList);
</script>
