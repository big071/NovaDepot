<template>
  <n-empty class="nd-empty-shell mt-4" :description="description">
    <template v-if="$slots.extra" #extra>
      <slot name="extra" />
    </template>
  </n-empty>
</template>

<script setup lang="ts">
import { NEmpty } from "naive-ui";

defineProps<{ description: string }>();
</script>
