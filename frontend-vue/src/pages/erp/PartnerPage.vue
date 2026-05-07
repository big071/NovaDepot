<template>
  <section class="space-y-5">
    <PageHeader eyebrow="ERP" title="往来单位" subtitle="统一维护供应商、客户与双向贸易伙伴。">
      <template #actions>
        <input ref="importInput" class="hidden" type="file" accept=".csv,text/csv" @change="onImportFile" />
        <n-button v-if="authStore.hasPermission('PARTNER_TEMPLATE_EXPORT')" class="nd-soft-focus" @click="downloadTemplate">CSV模板</n-button>
        <n-button v-if="authStore.hasPermission('PARTNER_IMPORT')" class="nd-soft-focus" @click="importInput?.click()">CSV导入</n-button>
        <n-button class="nd-soft-focus" :loading="loading" @click="loadList">鍒锋柊</n-button>
        <n-button v-if="canWrite" class="nd-soft-focus" type="primary" @click="openCreate">新增往来单位</n-button>
      </template>
      <template #meta>
        <span class="nd-pill">璁板綍鏁帮細{{ rows.length }}</span>
      </template>
    </PageHeader>

    <SearchForm :loading="loading" @search="loadList" @reset="resetFilters">
      <n-input v-model:value="filters.keyword" placeholder="鎼滅储缂栫爜銆佸悕绉般€佽仈绯讳汉" clearable />
      <n-select v-model:value="filters.partnerType" :options="filterTypeOptions" placeholder="鍗曚綅绫诲瀷" clearable />
    </SearchForm>

    <ErrorState v-if="errorText" :message="errorText" @retry="loadList" />
    <n-alert v-else-if="successText" class="nd-state-alert" type="success" :show-icon="false">{{ successText }}</n-alert>

    <DataTable title="Partner List" :subtitle="`${rows.length} records`" :columns="columns" :data="tableRows" :loading="loading">
      <template #empty>
        <EmptyState description="No partner data yet.">
          <template #extra>
            <n-button v-if="canWrite" class="nd-soft-focus" type="primary" @click="openCreate">Create Now</n-button>
          </template>
        </EmptyState>
      </template>
    </DataTable>

    <n-modal v-model:show="formVisible" preset="card" :title="editingId ? 'Edit Partner' : 'New Partner'" class="max-w-xl">
      <div class="grid gap-3 md:grid-cols-2">
        <n-input v-model:value="form.partnerCode" placeholder="鍗曚綅缂栫爜" />
        <n-input v-model:value="form.partnerName" placeholder="鍗曚綅鍚嶇О" />
        <n-select v-model:value="form.partnerType" :options="typeOptions" placeholder="鍗曚綅绫诲瀷" />
        <n-input v-model:value="form.contactName" placeholder="Contact" />
        <n-input v-model:value="form.phone" placeholder="鐢佃瘽" />
        <n-input v-model:value="form.address" placeholder="鍦板潃" />
        <n-input v-model:value="form.remark" class="md:col-span-2" placeholder="澶囨敞" type="textarea" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" @click="formVisible = false">鍙栨秷</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="submitting" :disabled="!canSubmit" @click="save">淇濆瓨</n-button>
        </div>
      </template>
    </n-modal>

    <ConfirmDialog
      v-model:show="confirmVisible"
      :title="pendingPartner?.status === 'ACTIVE' ? 'Disable Partner' : 'Enable Partner'"
      :content="`Confirm ${pendingPartner?.status === 'ACTIVE' ? 'disable' : 'enable'} ${pendingPartner?.partnerName || 'this partner'}?`"
      positive-text="纭"
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
  { label: "Supplier", value: "SUPPLIER" },
  { label: "瀹㈡埛", value: "CUSTOMER" },
  { label: "Both", value: "BOTH" }
];

const filterTypeOptions = [{ label: "鍏ㄩ儴绫诲瀷", value: "" }, ...typeOptions];

const canWrite = computed(() => authStore.hasPermission("PARTNER_CREATE") || authStore.hasPermission("PARTNER_UPDATE"));
const canSubmit = computed(() => Boolean(form.partnerCode.trim() && form.partnerName.trim() && form.partnerType) && !submitting.value);
const tableRows = computed(() => rows.value as unknown as Array<Record<string, unknown>>);

const columns: DataTableColumns<Record<string, unknown>> = [
  { title: "缂栫爜", key: "partnerCode" },
  { title: "鍚嶇О", key: "partnerName" },
  { title: "绫诲瀷", key: "partnerType", render: (row) => typeLabel(String(row.partnerType || "")) },
  { title: "Contact", key: "contactName", render: (row) => String(row.contactName || "-") },
  { title: "鐢佃瘽", key: "phone", render: (row) => String(row.phone || "-") },
  { title: "Status", key: "status", render: (row) => h(StatusBadge, { status: String(row.status || "") }) },
  {
    title: "鎿嶄綔",
    key: "actions",
    width: 180,
    render: (row) =>
      h("div", { class: "flex gap-2" }, [
        authStore.hasPermission("PARTNER_UPDATE") ? h(NButton, { class: "nd-soft-focus", size: "small", onClick: () => openEdit(row as unknown as Partner) }, { default: () => "缂栬緫" }) : null,
        authStore.hasPermission("PARTNER_UPDATE")
          ? h(
              NButton,
              { class: "nd-soft-focus", size: "small", type: String(row.status) === "ACTIVE" ? "warning" : "primary", onClick: () => openStatusConfirm(row as unknown as Partner) },
              { default: () => (String(row.status) === "ACTIVE" ? "鍋滅敤" : "鍚敤") }
            )
          : null
      ])
  }
];

function typeLabel(value: string) {
  if (value === "SUPPLIER") return "Supplier";
  if (value === "CUSTOMER") return "瀹㈡埛";
  if (value === "BOTH") return "Both";
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
    errorText.value = error instanceof Error ? error.message : "Partner loading failed";
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
      successText.value = "寰€鏉ュ崟浣嶅凡鏇存柊";
    } else {
      await erpApi.createPartner(payload);
      successText.value = "寰€鏉ュ崟浣嶅凡鍒涘缓";
    }
    formVisible.value = false;
    await loadList();
    message.success(successText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "淇濆瓨澶辫触";
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
      successText.value = "寰€鏉ュ崟浣嶅凡鍋滅敤";
    } else {
      await erpApi.enablePartner(row.id);
      successText.value = "寰€鏉ュ崟浣嶅凡鍚敤";
    }
    await loadList();
    message.success(successText.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "Status update failed";
    message.error(errorText.value);
  } finally {
    pendingPartner.value = null;
  }
}

onMounted(loadList);
</script>
