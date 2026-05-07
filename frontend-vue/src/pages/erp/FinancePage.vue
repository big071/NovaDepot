<template>
  <section class="space-y-5">
    <PageHeader eyebrow="ERP" title="Finance Ledger" subtitle="Lightweight payable and receivable balances for confirmed purchase and sales orders.">
      <template #actions>
        <n-button class="nd-soft-focus" :loading="loading" @click="loadAll">Refresh</n-button>
      </template>
      <template #meta>
        <span class="nd-pill">Payables: {{ payables.length }}</span>
        <span class="nd-pill">Receivables: {{ receivables.length }}</span>
      </template>
    </PageHeader>

    <SearchForm :loading="loading" @search="loadAll" @reset="resetFilters">
      <n-select v-model:value="activeTab" :options="tabOptions" placeholder="Ledger" />
      <n-select v-model:value="filters.status" :options="statusOptions" placeholder="Status" clearable />
    </SearchForm>

    <ErrorState v-if="errorText" :message="errorText" @retry="loadAll" />
    <n-alert v-else-if="successText" class="nd-state-alert" type="success" :show-icon="false">{{ successText }}</n-alert>

    <DataTable
      v-if="activeTab === 'payables'"
      title="Payable ledger"
      :subtitle="`Total ${filteredPayables.length}`"
      :columns="payableColumns"
      :data="filteredPayables"
      :loading="loading"
    >
      <template #empty><EmptyState description="No payable ledgers" /></template>
    </DataTable>

    <DataTable
      v-else
      title="Receivable ledger"
      :subtitle="`Total ${filteredReceivables.length}`"
      :columns="receivableColumns"
      :data="filteredReceivables"
      :loading="loading"
    >
      <template #empty><EmptyState description="No receivable ledgers" /></template>
    </DataTable>

    <n-modal v-model:show="registerVisible" preset="card" :title="registerTitle" class="max-w-xl">
      <div class="space-y-3">
        <n-input-number v-model:value="registerForm.amount" :min="0.01" :precision="2" placeholder="Amount" class="w-full" />
        <n-input v-model:value="registerForm.paidAt" placeholder="Date yyyy-MM-dd" />
        <n-input v-model:value="registerForm.method" placeholder="Method" />
        <n-input v-model:value="registerForm.remark" placeholder="Remark" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" @click="registerVisible = false">Cancel</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="submitting" :disabled="!registerForm.amount" @click="submitRegistration">Submit</n-button>
        </div>
      </template>
    </n-modal>
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from "vue";
import { NAlert, NButton, NInput, NInputNumber, NModal, NSelect, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import PageHeader from "@/components/shared/PageHeader.vue";
import DataTable from "@/components/shared/DataTable.vue";
import SearchForm from "@/components/shared/SearchForm.vue";
import StatusBadge from "@/components/shared/StatusBadge.vue";
import EmptyState from "@/components/shared/EmptyState.vue";
import ErrorState from "@/components/shared/ErrorState.vue";
import { financeApi, type Payable, type Receivable } from "@/services/finance";
import { erpApi, type Partner } from "@/services/erp";
import { wmsApi, type Warehouse } from "@/services/wms";
import { useAuthStore } from "@/stores/auth";

const message = useMessage();
const authStore = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const errorText = ref("");
const successText = ref("");
const activeTab = ref<"payables" | "receivables">("payables");
const payables = ref<Payable[]>([]);
const receivables = ref<Receivable[]>([]);
const partners = ref<Partner[]>([]);
const warehouses = ref<Warehouse[]>([]);
const registerVisible = ref(false);
const registerType = ref<"payable" | "receivable">("payable");
const registerTargetId = ref("");
const registerForm = reactive({ amount: null as number | null, paidAt: "", method: "MANUAL", remark: "" });
const filters = reactive({ status: null as string | null });

const tabOptions = [
  { label: "Payables", value: "payables" },
  { label: "Receivables", value: "receivables" }
];
const statusOptions = ["UNPAID", "PARTIALLY_PAID", "PAID", "CANCELLED"].map((value) => ({ label: value, value }));
const canPay = computed(() => authStore.hasPermission("FINANCE_PAYMENT_REGISTER"));
const canReceive = computed(() => authStore.hasPermission("FINANCE_RECEIPT_REGISTER"));
const registerTitle = computed(() => (registerType.value === "payable" ? "Register payment" : "Register receipt"));
const filteredPayables = computed(() => payables.value.filter((row) => !filters.status || row.status === filters.status) as unknown as Array<Record<string, unknown>>);
const filteredReceivables = computed(() => receivables.value.filter((row) => !filters.status || row.status === filters.status) as unknown as Array<Record<string, unknown>>);

const payableColumns: DataTableColumns<Record<string, unknown>> = [
  { title: "Ledger No", key: "payableNo" },
  { title: "Source", key: "sourceOrderNo" },
  { title: "Partner", key: "partnerId", render: (row) => partnerName(String(row.partnerId || "")) },
  { title: "Warehouse", key: "warehouseId", render: (row) => warehouseName(String(row.warehouseId || "")) },
  { title: "Total", key: "totalAmount" },
  { title: "Paid", key: "paidAmount" },
  { title: "Balance", key: "balanceAmount" },
  { title: "Status", key: "status", render: (row) => h(StatusBadge, { status: String(row.status || "") }) },
  {
    title: "Actions",
    key: "actions",
    width: 160,
    render: (row) => {
      const item = row as unknown as Payable;
      return canPay.value && !["PAID", "CANCELLED"].includes(item.status)
        ? h(NButton, { class: "nd-soft-focus", size: "small", type: "primary", onClick: () => openRegister("payable", item) }, { default: () => "Register" })
        : null;
    }
  }
];

const receivableColumns: DataTableColumns<Record<string, unknown>> = [
  { title: "Ledger No", key: "receivableNo" },
  { title: "Source", key: "sourceOrderNo" },
  { title: "Partner", key: "partnerId", render: (row) => partnerName(String(row.partnerId || "")) },
  { title: "Warehouse", key: "warehouseId", render: (row) => warehouseName(String(row.warehouseId || "")) },
  { title: "Total", key: "totalAmount" },
  { title: "Received", key: "receivedAmount" },
  { title: "Balance", key: "balanceAmount" },
  { title: "Status", key: "status", render: (row) => h(StatusBadge, { status: String(row.status || "") }) },
  {
    title: "Actions",
    key: "actions",
    width: 160,
    render: (row) => {
      const item = row as unknown as Receivable;
      return canReceive.value && !["PAID", "CANCELLED"].includes(item.status)
        ? h(NButton, { class: "nd-soft-focus", size: "small", type: "primary", onClick: () => openRegister("receivable", item) }, { default: () => "Register" })
        : null;
    }
  }
];

function partnerName(id: string) {
  return partners.value.find((item) => String(item.id) === String(id))?.partnerName || id || "-";
}

function warehouseName(id: string) {
  return warehouses.value.find((item) => String(item.id) === String(id))?.warehouseName || id || "-";
}

function resetFilters() {
  filters.status = null;
  loadAll();
}

function openRegister(type: "payable" | "receivable", item: Payable | Receivable) {
  registerType.value = type;
  registerTargetId.value = item.id;
  registerForm.amount = Number(item.balanceAmount || 0);
  registerForm.paidAt = new Date().toISOString().slice(0, 10);
  registerForm.method = "MANUAL";
  registerForm.remark = "";
  registerVisible.value = true;
}

async function submitRegistration() {
  if (!registerForm.amount) return;
  submitting.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    const payload = {
      amount: registerForm.amount,
      paidAt: registerForm.paidAt || undefined,
      method: registerForm.method || "MANUAL",
      remark: registerForm.remark || undefined
    };
    if (registerType.value === "payable") {
      await financeApi.registerPayment(registerTargetId.value, payload);
      successText.value = "Payment registered.";
    } else {
      await financeApi.registerReceipt(registerTargetId.value, payload);
      successText.value = "Receipt registered.";
    }
    registerVisible.value = false;
    await loadAll();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "Registration failed";
    message.error(errorText.value);
  } finally {
    submitting.value = false;
  }
}

async function loadAll() {
  loading.value = true;
  errorText.value = "";
  try {
    const [payableRows, receivableRows, partnerRows, warehouseRows] = await Promise.all([
      authStore.hasPermission("FINANCE_PAYABLE_READ") ? financeApi.listPayables() : Promise.resolve([]),
      authStore.hasPermission("FINANCE_RECEIVABLE_READ") ? financeApi.listReceivables() : Promise.resolve([]),
      erpApi.listPartners(),
      wmsApi.listWarehouses()
    ]);
    payables.value = payableRows;
    receivables.value = receivableRows;
    partners.value = partnerRows;
    warehouses.value = warehouseRows;
    if (activeTab.value === "payables" && !authStore.hasPermission("FINANCE_PAYABLE_READ")) {
      activeTab.value = "receivables";
    }
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "Failed to load finance ledgers";
  } finally {
    loading.value = false;
  }
}

onMounted(loadAll);
</script>
