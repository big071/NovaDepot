<template>
  <section class="space-y-5">
    <PageHeader eyebrow="ERP" title="往来单位" subtitle="统一维护供应商、客户与双向贸易伙伴。">
      <template #actions>
        <input ref="importInput" class="hidden" type="file" accept=".csv,text/csv" @change="onImportFile" />
        <n-button v-if="authStore.hasPermission('PARTNER_TEMPLATE_EXPORT')" class="nd-soft-focus" @click="downloadTemplate">CSV模板</n-button>
        <n-button v-if="authStore.hasPermission('PARTNER_IMPORT')" class="nd-soft-focus" @click="importInput?.click()">CSV导入</n-button>
        <n-button class="nd-soft-focus" :loading="loading" @click="loadList">刷新</n-button>
        <n-button v-if="canWrite" class="nd-soft-focus" type="primary" @click="openCreate">新增往来单位</n-button>
      </template>
      <template #meta>
        <span class="nd-pill">记录数：{{ rows.length }}</span>
      </template>
    </PageHeader>

    <SearchForm :loading="loading" @search="loadList" @reset="resetFilters">
      <n-input v-model:value="filters.keyword" placeholder="搜索编码、名称、联系人" clearable />
      <n-select v-model:value="filters.partnerType" :options="filterTypeOptions" placeholder="单位类型" clearable />
    </SearchForm>

    <ErrorState v-if="errorText" :message="errorText" @retry="loadList" />
    <n-alert v-else-if="successText" class="nd-state-alert" type="success" :show-icon="false">{{ successText }}</n-alert>

    <DataTable title="往来单位列表" :subtitle="`${rows.length} 条记录`" :columns="columns" :data="tableRows" :loading="loading">
      <template #empty>
        <EmptyState description="No partner data yet.">
          <template #extra>
            <n-button v-if="canWrite" class="nd-soft-focus" type="primary" @click="openCreate">立即新增</n-button>
          </template>
        </EmptyState>
      </template>
    </DataTable>

    <n-modal v-model:show="formVisible" preset="card" :title="editingId ? '编辑往来单位' : '新增往来单位'" class="max-w-xl">
      <div class="grid gap-3 md:grid-cols-2">
        <n-input v-model:value="form.partnerCode" placeholder="单位编码" />
        <n-input v-model:value="form.partnerName" placeholder="单位名称" />
        <n-select v-model:value="form.partnerType" :options="typeOptions" placeholder="单位类型" />
        <n-input v-model:value="form.contactName" placeholder="联系人" />
        <n-input v-model:value="form.phone" placeholder="电话" />
        <n-input v-model:value="form.address" placeholder="地址" />
        <n-input v-model:value="form.remark" class="md:col-span-2" placeholder="备注" type="textarea" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" @click="formVisible = false">取消</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="submitting" :disabled="!canSubmit" @click="save">保存</n-button>
        </div>
      </template>
    </n-modal>

    <ConfirmDialog
      v-model:show="confirmVisible"
      :title="pendingPartner?.status === 'ACTIVE' ? '停用往来单位' : '启用往来单位'"
      :content="`确认${pendingPartner?.status === 'ACTIVE' ? '停用' : '启用'} ${pendingPartner?.partnerName || '该往来单位'}？`"
      positive-text="确认"
      @confirm="applyStatusChange"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from "vue";
import { NAlert, NButton, NInput, NModal, NSelect, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import PageHeader from "@/components/shared/PageHeader.vue";
import DataTable from "@/components/shared/DataTable.vue";
import SearchForm from "@/components/shared/SearchForm.vue";
import StatusBadge from "@/components/shared/StatusBadge.vue";
import ConfirmDialog from "@/components/shared/ConfirmDialog.vue";
import EmptyState from "@/components/shared/EmptyState.vue";
import ErrorState from "@/components/shared/ErrorState.vue";
import { erpApi, type Partner } from "@/services/erp";
import { useAuthStore } from "@/stores/auth";

const message = useMessage();
const authStore = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const errorText = ref("");
const successText = ref("");
const formVisible = ref(false);
const confirmVisible = ref(false);
const editingId = ref<string | null>(null);
const pendingPartner = ref<Partner | null>(null);
const importInput = ref<HTMLInputElement | null>(null);
const rows = ref<Partner[]>([]);

const filters = reactive({
  keyword: "",
  partnerType: null as string | null
});

const form = reactive({
  partnerCode: "",
  partnerName: "",
  partnerType: "SUPPLIER",
  contactName: "",
  phone: "",
  address: "",
  remark: ""
});

const typeOptions = [
  { label: "供应商", value: "SUPPLIER" },
  { label: "客户", value: "CUSTOMER" },
  { label: "双向", value: "BOTH" }
];

const filterTypeOptions = [{ label: "全部类型", value: "" }, ...typeOptions];

const canWrite = computed(() => authStore.hasPermission("PARTNER_CREATE") || authStore.hasPermission("PARTNER_UPDATE"));
const canSubmit = computed(() => Boolean(form.partnerCode.trim() && form.partnerName.trim() && form.partnerType) && !submitting.value);
const tableRows = computed(() => rows.value as unknown as Array<Record<string, unknown>>);

const columns: DataTableColumns<Record<string, unknown>> = [
  { title: "编码", key: "partnerCode" },
  { title: "名称", key: "partnerName" },
  { title: "类型", key: "partnerType", render: (row) => typeLabel(String(row.partnerType || "")) },
  { title: "联系人", key: "contactName", render: (row) => String(row.contactName || "-") },
  { title: "电话", key: "phone", render: (row) => String(row.phone || "-") },
  { title: "Status", key: "status", render: (row) => h(StatusBadge, { status: String(row.status || "") }) },
  {
    title: "操作",
    key: "actions",
    width: 180,
    render: (row) =>
      h("div", { class: "flex gap-2" }, [
        authStore.hasPermission("PARTNER_UPDATE") ? h(NButton, { class: "nd-soft-focus", size: "small", onClick: () => openEdit(row as unknown as Partner) }, { default: () => "编辑" }) : null,
        authStore.hasPermission("PARTNER_UPDATE")
          ? h(
              NButton,
              { class: "nd-soft-focus", size: "small", type: String(row.status) === "ACTIVE" ? "warning" : "primary", onClick: () => openStatusConfirm(row as unknown as Partner) },
              { default: () => (String(row.status) === "ACTIVE" ? "停用" : "启用") }
            )
          : null
      ])
  }
];

function typeLabel(value: string) {
  if (value === "SUPPLIER") return "供应商";
  if (value === "CUSTOMER") return "客户";
  if (value === "BOTH") return "双向";
  return value || "-";
}

function resetFilters() {
  filters.keyword = "";
  filters.partnerType = null;
  loadList();
}

function resetForm() {
  editingId.value = null;
  form.partnerCode = "";
  form.partnerName = "";
  form.partnerType = "SUPPLIER";
  form.contactName = "";
  form.phone = "";
  form.address = "";
  form.remark = "";
}

function openCreate() {
  resetForm();
  formVisible.value = true;
}

function openEdit(row: Partner) {
  editingId.value = row.id;
  form.partnerCode = row.partnerCode;
  form.partnerName = row.partnerName;
  form.partnerType = row.partnerType;
  form.contactName = row.contactName || "";
  form.phone = row.phone || "";
  form.address = row.address || "";
  form.remark = row.remark || "";
  formVisible.value = true;
}

function openStatusConfirm(row: Partner) {
  pendingPartner.value = row;
  confirmVisible.value = true;
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
    downloadText("partners-import-template.csv", await erpApi.exportPartnerImportTemplate());
  } catch (error) {
    message.error(error instanceof Error ? error.message : "模板下载失败");
  }
}

async function onImportFile(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;
  try {
    const result = await erpApi.importPartners(await file.text());
    successText.value = `CSV导入完成：成功 ${result.successRows}，失败 ${result.failedRows}，跳过 ${result.skippedRows}`;
    if (result.reportId) {
      downloadText(`partner-import-errors-${result.reportId}.csv`, await erpApi.getPartnerImportErrorReport(result.reportId));
    }
    await loadList();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "CSV导入失败");
  } finally {
    if (importInput.value) importInput.value.value = "";
  }
}
async function loadList() {
  loading.value = true;
  errorText.value = "";
  try {
    rows.value = await erpApi.listPartners({
      keyword: filters.keyword,
      partnerType: filters.partnerType || undefined
    });
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "往来单位加载失败";
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
    const payload = { ...form, partnerType: form.partnerType as Partner["partnerType"] };
    if (editingId.value) {
      await erpApi.updatePartner(editingId.value, payload);
      successText.value = "往来单位已更新";
    } else {
      await erpApi.createPartner(payload);
      successText.value = "往来单位已创建";
    }
    formVisible.value = false;
    await loadList();
    message.success(successText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "保存失败";
    message.error(errorText.value);
  } finally {
    submitting.value = false;
  }
}

async function applyStatusChange() {
  if (!pendingPartner.value) return;
  const row = pendingPartner.value;
  confirmVisible.value = false;
  try {
    if (row.status === "ACTIVE") {
      await erpApi.disablePartner(row.id);
      successText.value = "往来单位已停用";
    } else {
      await erpApi.enablePartner(row.id);
      successText.value = "往来单位已启用";
    }
    await loadList();
    message.success(successText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "状态更新失败";
    message.error(errorText.value);
  } finally {
    pendingPartner.value = null;
  }
}

onMounted(loadList);
</script>
