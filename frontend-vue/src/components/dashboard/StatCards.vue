<template>
  <section class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
    <article
      v-for="card in cards"
      :key="card.title"
      class="glass-card rounded-2xl border border-border p-4 shadow-card transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl"
    >
      <p class="text-sm text-text-secondary">{{ card.title }}</p>
      <p class="mt-2 text-2xl font-semibold tracking-tight">{{ card.value }}</p>
      <p class="mt-1 text-xs" :class="card.trend >= 0 ? 'text-success' : 'text-danger'">
        {{ card.trend >= 0 ? '+' : '' }}{{ card.trend }}%
      </p>
    </article>
  </section>
</template>

<script setup lang="ts">
const cards = [
  { title: "库存总量", value: "12,460", trend: 5.4 },
  { title: "待处理入库", value: "19", trend: -2.1 },
  { title: "待处理出库", value: "26", trend: 3.8 },
  { title: "AI 对话量", value: "342", trend: 11.6 }
];
</script>
