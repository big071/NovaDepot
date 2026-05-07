<template>
  <section class="space-y-4">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">OPS</p>
          <h1 class="nd-page-title">数据备份</h1>
          <p class="nd-page-subtitle">管理员查看本地备份记录，并可手动触发一次备份记录。</p>
        </div>
        <div class="flex gap-2">
          <n-button class="nd-soft-focus" :loading="loading" @click="loadList">刷新</n-button>
          <n-button class="nd-soft-focus" type="primary" :loading="running" @click="runBackup">手动备份</n-button>
        </div>
      </div>
    </header>

    <n-alert v-if="errorText" type="error" :show-icon="false">{{ errorText }}</n-alert>
    <n-alert v-else-if="successText" type="success" :show-icon="false">{{ successText }}</n-alert>

    <article class="nd-table-shell">
      <div class="nd-table-head"><h3 class="nd-section-title">备份记录</h3></div>
      <div class="nd-table-body">
        <n-data-table :columns="columns" :data="rows" :loading="loading" :bordered="false" />
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { h, onMounted, ref } from "vue";
import { NButton, NDataTable, NTag, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { backupApi, type BackupRecord } from "@/services/backup";

const message = useMessage();
const rows = ref<BackupRecord[]>([]);
const loading = ref(false);
const running = ref(false);
const errorText = ref("");
const successText = ref("");

const columns: DataTableColumns<BackupRecord> = [
  { title: "备份编号", key: "backupNo", minWidth: 180 },
  { title: "文件名", key: "fileName", render: (row) => row.fileName || "-" },
  { title: "大小", key: "fileSize", width: 100, render: (row) => row.fileSize ? `${row.fileSize} B` : "-" },
  { title: "状态", key: "status", width: 120, render: (row) => h(NTag, { type: row.status === "SUCCESS" ? "success" : row.status === "FAILED" ? "error" : "warning", bordered: false }, { default: () => row.status }) },
  { title: "开始时间", key: "startedAt", minWidth: 170 },
  { title: "完成时间", key: "finishedAt", minWidth: 170, render: (row) => row.finishedAt || "-" },
  { title: "错误", key: "errorMessage", render: (row) => row.errorMessage || "-" }
];

async function loadList() {
  loading.value = true;
  errorText.value = "";
  try {
    rows.value = await backupApi.list();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "备份记录加载失败";
  } finally {
    loading.value = false;
  }
}

async function runBackup() {
  running.value = true;
  errorText.value = "";
  try {
    const result = await backupApi.run();
    successText.value = `备份已触发：${result.backupNo}`;
    message.success(successText.value);
    await loadList();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "备份触发失败";
  } finally {
    running.value = false;
  }
}

onMounted(loadList);
</script>
