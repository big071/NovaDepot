<template>
  <n-alert class="nd-state-alert" type="error" :show-icon="false">
    <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
      <span>{{ message }}</span>
      <n-button v-if="retryable" class="nd-soft-focus" size="small" @click="$emit('retry')">{{ retryText }}</n-button>
    </div>
  </n-alert>
</template>

<script setup lang="ts">
import { NAlert, NButton } from "naive-ui";

withDefaults(
  defineProps<{
    message: string;
    retryable?: boolean;
    retryText?: string;
  }>(),
  {
    retryable: true,
    retryText: "重试"
  }
);

defineEmits<{
  retry: [];
}>();
</script>
