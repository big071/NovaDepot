<template>
  <section class="rounded-lg border border-border bg-surface p-4 shadow-sm">
    <div class="grid gap-3 md:grid-cols-[1fr_auto] md:items-end">
      <div class="grid gap-3 md:grid-cols-3">
        <slot />
      </div>
      <div class="flex flex-wrap justify-end gap-2">
        <n-button class="nd-soft-focus" :disabled="loading" @click="$emit('reset')">{{ resetText }}</n-button>
        <n-button class="nd-soft-focus" type="primary" :loading="loading" @click="$emit('search')">{{ searchText }}</n-button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { NButton } from "naive-ui";

withDefaults(
  defineProps<{
    loading?: boolean;
    searchText?: string;
    resetText?: string;
  }>(),
  {
    searchText: "查询",
    resetText: "重置"
  }
);

defineEmits<{
  search: [];
  reset: [];
}>();
</script>
