<template>
  <section class="glass-card rounded-2xl border border-border p-6 shadow-card">
    <h1 class="text-xl font-semibold">{{ title }}</h1>
    <p class="mt-2 text-sm text-text-secondary">
      该页面将在后续阶段按计划迁移。本阶段仅完成 Vue 3 骨架与路由接入。
    </p>
  </section>
</template>

<script setup lang="ts">
defineProps<{ title: string }>();
</script>
