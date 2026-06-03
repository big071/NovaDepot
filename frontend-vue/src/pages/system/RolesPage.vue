<template>
  <section class="space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">System</p>
          <h1 class="nd-page-title">角色管理</h1>
          <p class="nd-page-subtitle">维护自定义角色和权限分配，内置角色仅允许查看。</p>
        </div>
        <div class="flex items-center gap-2">
          <n-button class="nd-soft-focus" :loading="loading" @click="loadAll">刷新</n-button>
          <n-button type="primary" class="nd-soft-focus" @click="openCreate">新建角色</n-button>
        </div>
      </div>
      <div class="nd-hero-meta">
        <span class="nd-pill">角色数：{{ rows.length }}</span>
        <span class="nd-pill">权限数：{{ permissions.length }}</span>
      </div>
    </header>

    <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">{{ errorText }}</n-alert>
    <n-alert v-else-if="lastSuccessText" class="nd-state-alert" type="success" :show-icon="false">{{ lastSuccessText }}</n-alert>

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h3 class="nd-section-title">角色列表</h3>
          <p class="nd-section-subtitle">自定义角色可编辑，内置角色用于保护演示账号权限基线。</p>
        </div>
      </div>
      <div class="nd-table-body">
        <n-data-table :columns="columns" :data="rows" :loading="loading" :bordered="false" />
      </div>
    </article>

    <n-modal v-model:show="detailVisible" preset="card" title="角色详情" class="max-w-3xl">
      <div v-if="selectedRole" class="space-y-4">
        <div class="grid gap-3 md:grid-cols-2">
          <n-input :value="selectedRole.roleCode" disabled />
          <n-input :value="selectedRole.roleName" disabled />
        </div>
        <div class="flex flex-wrap gap-2">
          <n-tag v-for="permission in selectedRole.permissions ?? []" :key="String(permission.id)" size="small">
            {{ permission.permCode }}
          </n-tag>
        </div>
      </div>
    </n-modal>

    <n-modal v-model:show="editorVisible" preset="card" :title="editingId ? '编辑角色' : '新建角色'" class="max-w-4xl">
      <div class="space-y-4">
        <div class="grid gap-3 md:grid-cols-2">
          <n-input v-model:value="form.roleCode" :disabled="Boolean(editingId)" placeholder="ROLE_CODE" />
          <n-input v-model:value="form.roleName" placeholder="角色名称" />
          <n-select v-model:value="form.dataScope" :options="dataScopeOptions" />
          <n-select v-model:value="form.status" :options="statusOptions" />
        </div>
        <div class="max-h-80 overflow-y-auto rounded-lg border border-border p-3">
          <n-checkbox-group v-model:value="selectedPermissionIds">
            <div class="grid gap-2 md:grid-cols-2">
              <n-checkbox v-for="permission in permissions" :key="String(permission.id)" :value="String(permission.id)">
                <span class="font-medium">{{ permission.permCode }}</span>
                <span class="ml-2 text-text-secondary">{{ permission.permName }}</span>
              </n-checkbox>
            </div>
          </n-checkbox-group>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button class="nd-soft-focus" :disabled="submitting" @click="editorVisible = false">取消</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="submitting" :disabled="!canSubmit" @click="submitRole">
            保存
          </n-button>
        </div>
      </template>
    </n-modal>
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from "vue";
import { NAlert, NButton, NCheckbox, NCheckboxGroup, NDataTable, NInput, NModal, NSelect, NTag, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { systemApi, type PermissionItem, type RoleItem, type RoleSavePayload } from "@/services/system";

const message = useMessage();
const loading = ref(false);
const submitting = ref(false);
const rows = ref<RoleItem[]>([]);
const permissions = ref<PermissionItem[]>([]);
const errorText = ref("");
const lastSuccessText = ref("");
const detailVisible = ref(false);
const editorVisible = ref(false);
const selectedRole = ref<RoleItem | null>(null);
const editingId = ref<string | number | null>(null);
const selectedPermissionIds = ref<string[]>([]);

const form = reactive<RoleSavePayload>({
  roleCode: "",
  roleName: "",
  dataScope: "ALL",
  status: "ACTIVE",
  permissionIds: []
});

const dataScopeOptions = [
  { label: "全部数据", value: "ALL" },
  { label: "仓库数据", value: "WAREHOUSE" },
  { label: "本人数据", value: "SELF" }
];

const statusOptions = [
  { label: "启用", value: "ACTIVE" },
  { label: "停用", value: "DISABLED" }
];

const canSubmit = computed(() => {
  return form.roleCode.trim().length > 0 && form.roleName.trim().length > 0 && !submitting.value;
});

const columns: DataTableColumns<RoleItem> = [
  { title: "角色编码", key: "roleCode" },
  { title: "角色名称", key: "roleName" },
  { title: "数据范围", key: "dataScope", width: 110 },
  { title: "状态", key: "status", width: 100 },
  { title: "权限数", key: "permissionCount", width: 100 },
  {
    title: "操作",
    key: "actions",
    width: 180,
    render: (row) =>
      h("div", { class: "flex gap-2" }, [
        h(NButton, { class: "nd-soft-focus", size: "small", onClick: () => openDetail(row) }, { default: () => "详情" }),
        h(
          NButton,
          { class: "nd-soft-focus", size: "small", disabled: row.builtIn, onClick: () => openEdit(row) },
          { default: () => "编辑" }
        )
      ])
  }
];

async function loadAll() {
  loading.value = true;
  errorText.value = "";
  try {
    const [roleRows, permissionRows] = await Promise.all([systemApi.listRoles(), systemApi.listPermissions()]);
    rows.value = roleRows;
    permissions.value = permissionRows;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "角色权限数据加载失败";
    message.error(errorText.value);
  } finally {
    loading.value = false;
  }
}

async function openDetail(row: RoleItem) {
  selectedRole.value = await systemApi.getRole(row.id);
  detailVisible.value = true;
}

async function openEdit(row: RoleItem) {
  const detail = await systemApi.getRole(row.id);
  editingId.value = detail.id;
  form.roleCode = detail.roleCode;
  form.roleName = detail.roleName;
  form.dataScope = detail.dataScope;
  form.status = detail.status;
  selectedPermissionIds.value = (detail.permissionIds ?? []).map(String);
  editorVisible.value = true;
}

function openCreate() {
  editingId.value = null;
  form.roleCode = `OPS_TEST_${Date.now()}`;
  form.roleName = "运维测试角色";
  form.dataScope = "ALL";
  form.status = "ACTIVE";
  selectedPermissionIds.value = [];
  editorVisible.value = true;
}

async function submitRole() {
  submitting.value = true;
  errorText.value = "";
  const payload: RoleSavePayload = {
    ...form,
    roleCode: form.roleCode.trim().toUpperCase(),
    roleName: form.roleName.trim(),
    permissionIds: selectedPermissionIds.value
  };
  try {
    if (editingId.value) {
      await systemApi.updateRole(editingId.value, payload);
      lastSuccessText.value = "角色已更新";
    } else {
      await systemApi.createRole(payload);
      lastSuccessText.value = "角色已创建";
    }
    message.success(lastSuccessText.value);
    editorVisible.value = false;
    await loadAll();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "角色保存失败";
    message.error(errorText.value);
  } finally {
    submitting.value = false;
  }
}

onMounted(loadAll);
</script>

