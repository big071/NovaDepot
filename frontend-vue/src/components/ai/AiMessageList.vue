<template>
  <div class="nd-chat-shell h-[460px] space-y-3 overflow-y-auto">
    <p v-if="loading" class="text-sm text-text-secondary">历史消息加载中...</p>
    <div v-else-if="messages.length === 0" class="space-y-2 text-sm text-text-secondary">
      <p>当前会话暂无消息，可先使用推荐问题快速体验。</p>
      <div class="flex flex-wrap gap-2">
        <n-button v-for="q in recommendedQuestions.slice(0, 3)" :key="`inline-${q}`" size="small" @click="$emit('ask', q)">
          {{ q }}
        </n-button>
      </div>
    </div>
    <AiMessageCard v-for="(item, index) in messages" :key="index" :message="item" :message-index="index" />
  </div>
</template>

<script setup lang="ts">
import { NButton } from "naive-ui";
import AiMessageCard from "@/components/ai/AiMessageCard.vue";
import type { ChatMessage } from "@/types/aiView";

defineProps<{
  loading: boolean;
  messages: ChatMessage[];
  recommendedQuestions: string[];
}>();

defineEmits<{
  ask: [question: string];
}>();
</script>
