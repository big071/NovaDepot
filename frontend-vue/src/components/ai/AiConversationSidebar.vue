<template>
  <article class="nd-table-shell">
    <div class="nd-table-head">
      <div>
        <h2 class="nd-section-title">AI 会话</h2>
        <p class="nd-section-subtitle">共 {{ conversations.length }} 个会话</p>
      </div>
      <div class="flex gap-2">
        <n-button class="nd-soft-focus" size="small" @click="$emit('create')">新建</n-button>
        <n-button class="nd-soft-focus" size="small" :loading="loading" @click="$emit('refresh')">刷新</n-button>
      </div>
    </div>
    <div class="nd-table-body space-y-2">
      <n-empty v-if="!loading && conversations.length === 0" description="暂无会话，可点击推荐问题快速开始。">
        <template #extra>
          <div class="flex flex-wrap gap-2">
            <n-button v-for="q in recommendedQuestions.slice(0, 2)" :key="q" size="small" @click="$emit('ask', q)">
              {{ q }}
            </n-button>
          </div>
        </template>
      </n-empty>
      <button
        v-for="item in conversations"
        :key="item.conversationNo"
        class="nd-chat-list-item"
        :class="activeConversationNo === item.conversationNo ? 'border-primary bg-primary/10' : 'border-border hover:border-primary/40 hover:bg-bg'"
        @click="$emit('select', item.conversationNo)"
      >
        <div class="flex items-center justify-between gap-2">
          <p class="font-medium">{{ item.conversationNo }}</p>
          <n-tag size="small" :bordered="false" :type="item.status === 'ARCHIVED' ? 'default' : 'info'">
            {{ item.status === 'ARCHIVED' ? '已归档' : item.provider }}
          </n-tag>
        </div>
        <p class="mt-1 text-xs text-text-secondary">
          {{ sceneLabelMap[item.scene] || item.scene }} · {{ item.lastActiveAt || item.startedAt }}
        </p>
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { NButton, NEmpty, NTag } from "naive-ui";
import type { AiConversation } from "@/services/ai";

defineProps<{
  conversations: AiConversation[];
  loading: boolean;
  activeConversationNo: string | null;
  recommendedQuestions: string[];
  sceneLabelMap: Record<string, string>;
}>();

defineEmits<{
  create: [];
  refresh: [];
  select: [conversationNo: string];
  ask: [question: string];
}>();
</script>
