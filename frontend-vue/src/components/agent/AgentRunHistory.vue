<template>
  <article class="nd-table-shell">
    <div class="nd-table-head">
      <div>
        <h3 class="nd-section-title">历史记录</h3>
        <p class="nd-section-subtitle">共 {{ total }} 条</p>
      </div>
      <n-button class="nd-soft-focus" :loading="loadingRuns" @click="$emit('refresh')">刷新历史</n-button>
    </div>
    <div class="nd-table-body">
      <n-data-table :columns="runColumns" :data="runs" :loading="loadingRuns" :bordered="false" />
      <div class="mt-4 flex justify-end" v-if="total > 0">
        <n-pagination
          :page="pageNo"
          :page-size="pageSize"
          :item-count="total"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          @update:page="(page) => $emit('page-change', page)"
          @update:page-size="(size) => $emit('page-size-change', size)"
        />
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { NButton, NDataTable, NPagination } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import type { AgentRunListItem } from "@/services/agent";

defineProps<{
  runs: AgentRunListItem[];
  total: number;
  pageNo: number;
  pageSize: number;
  loadingRuns: boolean;
  runColumns: DataTableColumns<AgentRunListItem>;
}>();

defineEmits<{
  refresh: [];
  "page-change": [page: number];
  "page-size-change": [pageSize: number];
}>();
</script>
