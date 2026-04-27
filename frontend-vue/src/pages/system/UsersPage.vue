<template>
  <section class="space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">System</p>
          <h1 class="nd-page-title">用户管理</h1>
          <p class="nd-page-subtitle">管理员可在此重置用户密码。</p>
        </div>
        <div class="flex items-center gap-2">
          <n-button class="nd-soft-focus" size="small" @click="toggleNewbieMode">
            {{ newbieMode ? "切换高级模式" : "切换新手模式" }}
          </n-button>
          <n-button class="nd-soft-focus" :loading="loading" @click="loadUsers">刷新</n-button>
        </div>
      </div>
      <div class="nd-hero-meta">
        <span class="nd-pill">用户数：{{ rows.length }}</span>
      </div>
    </header>
    <n-alert class="nd-state-alert" type="info" :show-icon="false">
      {{ newbieMode ? "新手模式已开启：默认展示用户名与姓名，保留重置密码核心操作。" : "高级模式已开启：展示完整字段用于排障与核对。" }}
    </n-alert>

    <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">{{ errorText }}</n-alert>
    <n-alert v-else-if="lastSuccessText" class="nd-state-alert" type="success" :show-icon="false">{{ lastSuccessText }}</n-alert>

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h3 class="nd-section-title">用户列表</h3>
          <p class="nd-section-subtitle">请选择用户并执行重置密码</p>
        </div>
      </div>
      <div class="nd-table-body">
        <n-data-table :columns="columns" :data="rows" :loading="loading" :bordered="false" />
        <n-empty v-if="!loading && rows.length === 0" class="nd-empty-shell mt-4" description="暂无用户数据" />
      </div>
    </article>

    <n-modal v-model:show="resetVisible" preset="card" title="管理员重置密码" class="max-w-lg">
      <div class="space-y-3">
        <n-alert type="warning" :show-icon="false">重置后该用户将被强制首次登录改密。</n-alert>
        <n-input :value="selectedUserLabel" disabled />
        <n-input v-model:value="newPassword" type="password" show-password-on="click" placeholder="新密码" />
        <n-input v-model:value="confirmPassword" type="password" show-password-on="click" placeholder="确认新密码" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" :disabled="resetSubmitting" @click="resetVisible = false">取消</n-button>
          <n-button
            class="nd-soft-focus"
            type="primary"
            :loading="resetSubmitting"
            :disabled="!canReset"
            @click="onResetPassword"
          >
            确认重置
          </n-button>
        </div>
      </template>
    </n-modal>
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from "vue";
import { NAlert, NButton, NDataTable, NEmpty, NInput, NModal, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { resetPasswordApi } from "@/services/auth";
import { systemApi, type UserItem } from "@/services/system";

const message = useMessage();
const loading = ref(false);
const resetSubmitting = ref(false);
const errorText = ref("");
const lastSuccessText = ref("");
const rows = ref<UserItem[]>([]);
const newbieMode = ref(true);

const resetVisible = ref(false);
const selectedUser = ref<UserItem | null>(null);
const newPassword = ref("");
const confirmPassword = ref("");

const selectedUserLabel = computed(() =>
  selectedUser.value ? `${selectedUser.value.username} (${selectedUser.value.realName || "-"})` : ""
);

const canReset = computed(() => {
  if (resetSubmitting.value || !selectedUser.value) return false;
  if (!newPassword.value || !confirmPassword.value) return false;
  return newPassword.value === confirmPassword.value;
});

const columns = computed<DataTableColumns<UserItem>>(() => {
  const base: DataTableColumns<UserItem> = [
    { title: "用户名", key: "username" },
    { title: "姓名", key: "realName", render: (row) => row.realName || "-" }
  ];
  if (!newbieMode.value) {
    base.unshift({ title: "用户ID", key: "id", width: 110 });
  }
  base.push({
    title: "操作",
    key: "actions",
    width: 130,
    render: (row) =>
      h(
        NButton,
        {
          class: "nd-soft-focus",
          size: "small",
          onClick: () => openResetModal(row)
        },
        { default: () => "重置密码" }
      )
  });
  return base;
});

function toggleNewbieMode() {
  newbieMode.value = !newbieMode.value;
}

function openResetModal(row: UserItem) {
  selectedUser.value = row;
  newPassword.value = "";
  confirmPassword.value = "";
  resetVisible.value = true;
}

async function loadUsers() {
  loading.value = true;
  errorText.value = "";
  try {
    rows.value = await systemApi.listUsers();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "用户列表加载失败";
    message.error(errorText.value);
  } finally {
    loading.value = false;
  }
}

async function onResetPassword() {
  if (!selectedUser.value) return;
  if (newPassword.value !== confirmPassword.value) {
    message.warning("两次输入的新密码不一致");
    return;
  }
  resetSubmitting.value = true;
  errorText.value = "";
  try {
    await resetPasswordApi(selectedUser.value.id, { newPassword: newPassword.value });
    lastSuccessText.value = `用户 ${selectedUser.value.username} 密码已重置`;
    message.success(lastSuccessText.value);
    resetVisible.value = false;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "重置密码失败";
    message.error(errorText.value);
  } finally {
    resetSubmitting.value = false;
  }
}

onMounted(loadUsers);
</script>
