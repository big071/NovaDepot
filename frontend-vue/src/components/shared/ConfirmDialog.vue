<template>
  <n-modal
    :show="show"
    preset="dialog"
    :title="title"
    :positive-text="positiveText"
    :negative-text="negativeText"
    @positive-click="$emit('confirm')"
    @negative-click="$emit('update:show', false)"
    @close="$emit('update:show', false)"
  >
    <p class="text-sm text-text-secondary">{{ content }}</p>
  </n-modal>
</template>

<script setup lang="ts">
import { NModal } from "naive-ui";

defineProps<{
  show: boolean;
  title: string;
  content: string;
  positiveText?: string;
  negativeText?: string;
}>();

defineEmits<{
  "update:show": [value: boolean];
  confirm: [];
}>();
</script>
