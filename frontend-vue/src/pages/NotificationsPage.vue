<template>
  <section class="space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Notifications</p>
          <h1 class="nd-page-title">通知中心</h1>
          <p class="nd-page-subtitle">集中查看 Agent 巡检和业务提醒，支持已读处理和关联页面跳转。</p>
        </div>
        <div class="flex gap-2">
          <n-button class="nd-soft-focus" :loading="loading" @click="loadList">刷新</n-button>
          <n-button class="nd-soft-focus" type="primary" :disabled="unreadCount === 0" @click="markAllRead">全部已读</n-button>
        </div>
      </div>
    </header>

    <n-alert v-if="errorText" type="error" :show-icon="false">
      {{ errorText }}
      <n-button size="small" class="ml-2" @click="loadList">重试</n-button>
    </n-alert>

    <section class="nd-toolbar">
      <div class="flex items-center gap-3">
        <n-switch v-model:value="unreadOnly" @update:value="onFilterChange" />
        <span class="text-sm text-text-secondary">仅看未读</span>
        <span class="rounded-full border border-border px-2 py-1 text-xs text-text-secondary">未读 {{ unreadCount }}</span>
      </div>
    </section>

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h3 class="nd-section-title">站内通知</h3>
          <p class="nd-section-subtitle">共 {{ total }} 条</p>
        </div>
      </div>
      <div class="nd-table-body">
        <div v-if="loading" class="space-y-3">
          <n-skeleton v-for="i in 5" :key="i" height="46px" />
        </div>
        <template v-else>
          <n-data-table :columns="columns" :data="rows" :bordered="false" />
          <n-empty v-if="rows.length === 0" class="nd-empty-shell mt-4" description="暂无通知" />
        </template>
        <div v-if="total > 0" class="mt-4 flex justify-end">
          <n-pagination
            :page="pageNo"
            :page-size="pageSize"
            :item-count="total"
            :page-sizes="[10, 20, 50]"
            show-size-picker
            @update:page="onPageChange"
            @update:page-size="onPageSizeChange"
          />
        </div>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { h, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { NAlert, NButton, NDataTable, NEmpty, NPagination, NSkeleton, NSwitch, NTag, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { notificationsApi, type NotificationItem } from "@/services/notifications";

const router = useRouter();
const message = useMessage();
const loading = ref(false);
const errorText = ref("");
const rows = ref<NotificationItem[]>([]);
const total = ref(0);
const unreadCount = ref(0);
const unreadOnly = ref(false);
const pageNo = ref(1);
const pageSize = ref(20);

const columns: DataTableColumns<NotificationItem> = [
  {
    title: "状态",
    key: "readFlag",
    width: 90,
    render: (row) => h(NTag, { type: isRead(row) ? "default" : "success", size: "small" }, { default: () => (isRead(row) ? "已读" : "未读") })
  },
  { title: "标题", key: "title", minWidth: 180, ellipsis: { tooltip: true } },
  { title: "内容", key: "content", minWidth: 280, ellipsis: { tooltip: true } },
  { title: "业务类型", key: "bizType", width: 140 },
  { title: "业务编号", key: "bizNo", width: 150, render: (row) => row.bizNo || "-" },
  { title: "发送时间", key: "sentAt", width: 180 },
  {
    title: "操作",
    key: "actions",
    width: 180,
    render: (row) =>
      h("div", { class: "flex gap-2" }, [
        h(NButton, { size: "small", secondary: true, onClick: () => openNotification(row) }, { default: () => "查看" }),
        h(NButton, { size: "small", disabled: isRead(row), onClick: () => markRead(row) }, { default: () => "已读" })
      ])
  }
];

function isRead(row: NotificationItem) {
  return row.readFlag === true || row.readFlag === 1;
}

async function loadList() {
  loading.value = true;
  errorText.value = "";
  try {
    const [page, count] = await Promise.all([
      notificationsApi.list({ unreadOnly: unreadOnly.value, pageNo: pageNo.value, pageSize: pageSize.value }),
      notificationsApi.unreadCount()
    ]);
    rows.value = page.list;
    total.value = page.total;
    unreadCount.value = count;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "通知加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

async function markRead(row: NotificationItem) {
  try {
    await notificationsApi.markRead(row.id);
    message.success("已标记为已读");
    window.dispatchEvent(new CustomEvent("novadepot:notifications-refresh"));
    await loadList();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "标记已读失败");
  }
}

async function markAllRead() {
  try {
    await notificationsApi.markAllRead();
    message.success("全部通知已读");
    window.dispatchEvent(new CustomEvent("novadepot:notifications-refresh"));
    await loadList();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "全部已读失败");
  }
}

async function openNotification(row: NotificationItem) {
  if (!isRead(row)) {
    await notificationsApi.markRead(row.id);
    window.dispatchEvent(new CustomEvent("novadepot:notifications-refresh"));
  }
  if (row.jumpPath) {
    router.push(row.jumpPath);
  }
}

function onFilterChange() {
  pageNo.value = 1;
  loadList();
}

function onPageChange(next: number) {
  pageNo.value = next;
  loadList();
}

function onPageSizeChange(next: number) {
  pageSize.value = next;
  pageNo.value = 1;
  loadList();
}

onMounted(loadList);
</script>
