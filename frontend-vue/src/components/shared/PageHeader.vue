<template>
  <header class="nd-hero">
    <div class="nd-hero-header">
      <div>
        <p v-if="eyebrow" class="text-xs uppercase tracking-[0.14em] text-text-secondary">{{ eyebrow }}</p>
        <h1 class="nd-page-title">{{ title }}</h1>
        <p v-if="subtitle" class="nd-page-subtitle">{{ subtitle }}</p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <slot name="actions" />
      </div>
    </div>
    <div v-if="$slots.meta" class="nd-hero-meta">
      <slot name="meta" />
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow?: string;
  title: string;
  subtitle?: string;
}>();
</script>
