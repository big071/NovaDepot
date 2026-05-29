<template>
  <article class="nd-table-shell">
    <div class="nd-table-head">
      <h3 class="nd-section-title">娑堟伅涓嶢I寤鸿</h3>
    </div>
    <div class="nd-table-body space-y-3">
      <div class="flex flex-wrap gap-2">
        <n-button class="nd-soft-focus" :disabled="!activeSessionId" :loading="loadingSuggestion" @click="$emit('load-suggestion')">鑾峰彇AI寤鸿</n-button>
        <n-button class="nd-soft-focus" :disabled="!activeSessionId || !suggestionFirst" @click="$emit('use-suggestion')">濂楃敤棣栨潯寤鸿</n-button>
        <n-button class="nd-soft-focus" :disabled="!activeSessionId" @click="$emit('transfer-human')">浜哄伐鎺ョ</n-button>
        <n-button class="nd-soft-focus" :disabled="!activeSessionId" @click="$emit('toggle-auto-reply')">
          鑷姩鍥炲锛歿{ autoReplyEnabled ? "寮€鍚? : "鍏抽棴" }}
        </n-button>
      </div>
      <article class="rounded-xl border border-border bg-bg/60 p-3 text-sm">
        <p class="font-medium">AI建议依据</p>
        <p class="mt-1 text-text-secondary">{{ (aiSuggestion?.basis || []).join("；") || "-" }}</p>
        <p class="mt-2 font-medium">工单分类建议：{{ aiSuggestion?.ticketCategorySuggestion || "-" }}</p>
        <p class="mt-1">优先级建议：{{ aiSuggestion?.prioritySuggestion || "-" }}</p>
        <p class="mt-1">SOP建议：{{ aiSuggestion?.sopSuggestion || "-" }}</p>
        <p class="mt-2 font-medium">知识引用来源</p>
        <div v-if="(aiSuggestion?.knowledgeRefs || []).length" class="mt-2 flex flex-wrap gap-2">
          <n-tag v-for="ref in aiSuggestion?.knowledgeRefs || []" :key="`${ref.type}-${ref.code || ref.title}`" :bordered="false" type="info">
            {{ ref.type }}：{{ ref.title }} / {{ ref.scene || "通用" }}
          </n-tag>
        </div>
        <p v-else class="mt-1 text-text-secondary">{{ aiSuggestion?.knowledgeFallbackNotice || "未命中知识库，当前建议来自规则回退。" }}</p>
        <p class="mt-2 text-xs text-text-secondary" v-if="aiSuggestion?.ruleConfigBasis">
          规则配置：自动回复 {{ aiSuggestion.ruleConfigBasis.autoReplyPriority }}；候选回复 {{ aiSuggestion.ruleConfigBasis.candidateReplyPriority }}
        </p>
      </article>
      <div class="max-h-[260px] space-y-2 overflow-y-auto rounded-xl border border-border bg-bg/40 p-3">
        <article v-for="msg in messages" :key="msg.id" class="rounded-lg border border-border bg-surface p-2 text-sm">
          <p class="text-xs text-text-secondary">{{ msg.sender }} 路 {{ msg.msgType }}</p>
          <p class="mt-1">{{ msg.content }}</p>
        </article>
      </div>
      <div class="flex gap-2">
        <n-input :value="inputText" placeholder="输入消息内容，回车发送" @update:value="$emit('update-input', $event)" @keyup.enter="$emit('send-agent-message')" />
        <n-button :loading="sending" :disabled="!activeSessionId || !inputText.trim()" @click="$emit('send-agent-message')">人工发送</n-button>
        <n-button type="primary" :loading="sending" :disabled="!activeSessionId || !inputText.trim()" @click="$emit('send-customer-message')">模拟客户提问</n-button>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { NButton, NInput, NTag } from "naive-ui";
import type { CsAiSuggestion, CsMessage } from "@/services/customerService";

defineProps<{
  activeSessionId: number | null;
  loadingSuggestion: boolean;
  suggestionFirst: string;
  autoReplyEnabled: boolean;
  aiSuggestion: CsAiSuggestion | null;
  messages: CsMessage[];
  inputText: string;
  sending: boolean;
}>();

defineEmits<{
  "load-suggestion": [];
  "use-suggestion": [];
  "transfer-human": [];
  "toggle-auto-reply": [];
  "update-input": [value: string];
  "send-agent-message": [];
  "send-customer-message": [];
}>();
</script>
