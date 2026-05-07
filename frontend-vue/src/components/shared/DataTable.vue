<template>
  <article class="nd-table-shell">
    <div class="nd-table-head">
      <div>
        <h3 class="nd-section-title">{{ title }}</h3>
        <p v-if="subtitle" class="nd-section-subtitle">{{ subtitle }}</p>
      </div>
      <slot name="actions" />
    </div>
    <div class="nd-table-body">
      <n-data-table class="nd-table" :columns="columns" :data="data" :loading="loading" :bordered="false" />
      <slot v-if="!loading && data.length === 0" name="empty" />
    </div>
  </article>
</template>

<script setup lang="ts">
import { NDataTable } from "naive-ui";
import type { DataTableColumns } from "naive-ui";

defineProps<{
  title: string;
  subtitle?: string;
  columns: DataTableColumns<Record<string, unknown>>;
  data: Array<Record<string, unknown>>;
  loading?: boolean;
}>();
</script>
