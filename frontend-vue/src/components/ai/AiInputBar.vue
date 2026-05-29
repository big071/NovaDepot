<template>
  <div class="mt-3 space-y-2">
    <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">{{ errorText }}</n-alert>
    <div class="nd-chat-composer">
      <div class="flex gap-2">
        <n-input
          class="nd-soft-focus"
          :value="modelValue"
          placeholder="输入问题并发送"
          :disabled="disabled"
          @update:value="$emit('update:modelValue', $event)"
          @keyup.enter="$emit('send')"
        />
        <n-button v-if="sending" class="nd-soft-focus" type="warning" @click="$emit('stop')">停止</n-button>
        <n-button v-else class="nd-soft-focus" type="primary" :disabled="!canSend" @click="$emit('send')">发送</n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NAlert, NButton, NInput } from "naive-ui";

defineProps<{
  modelValue: string;
  sending: boolean;
  canSend: boolean;
  disabled: boolean;
  errorText: string;
}>();

defineEmits<{
  "update:modelValue": [value: string];
  send: [];
  stop: [];
}>();
</script>
