<template>
  <div
    class="max-w-[85%] rounded-2xl px-3 py-2 text-sm leading-6"
    :class="message.role === 'user' ? 'ml-auto bg-primary text-white shadow-sm' : 'border border-border bg-surface text-text-primary'"
  >
    <p v-if="message.role === 'user'">{{ message.content }}</p>
    <div v-else class="nd-ai-rich space-y-3">
      <section
        v-for="section in parseAssistantContent(message.content)"
        :key="section.title || section.content.join('|')"
        class="rounded-lg border border-border bg-bg/40 p-3"
      >
        <div v-if="section.title" class="mb-2 flex items-center justify-between gap-2">
          <h3 class="text-sm font-semibold text-text-primary">{{ section.title }}</h3>
          <n-tag v-if="section.badge" size="small" :bordered="false" :type="section.badgeType">{{ section.badge }}</n-tag>
        </div>
        <ul v-if="section.items.length" :class="isBusinessCardSection(section) ? 'grid gap-2' : 'space-y-2'">
          <li v-for="itemText in section.items" :key="itemText" :class="isBusinessCardSection(section) ? 'nd-ai-action-card' : 'leading-6'">
            <div v-if="isBusinessCardSection(section)" class="flex items-start justify-between gap-2">
              <span class="leading-6" v-html="renderInlineMarkdown(itemText)"></span>
              <n-tag size="small" :bordered="false" :type="priorityType(itemText)">
                {{ priorityLabel(itemText) }}
              </n-tag>
            </div>
            <span v-else v-html="renderInlineMarkdown(itemText)"></span>
          </li>
        </ul>
        <p v-for="paragraph in section.content" :key="paragraph" class="leading-6" v-html="renderInlineMarkdown(paragraph)"></p>
      </section>

      <AiToolEvidencePanel :tool-calls="message.toolCalls" :message-index="messageIndex" />
    </div>
    <div v-if="message.role === 'assistant' && message.validationWarnings?.length" class="mt-2 space-y-1 text-xs text-warning">
      <p v-for="warning in message.validationWarnings" :key="warning">{{ warning }}</p>
    </div>
    <p v-if="message.role === 'assistant' && message.toolLimitReached" class="mt-2 text-xs text-warning">
      已达到本轮最多 5 次工具调用限制。
    </p>
    <p v-if="message.role === 'assistant' && message.status && message.status !== 'COMPLETED'" class="mt-1 text-xs text-text-secondary">
      {{ statusLabel(message.status) }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { NTag } from "naive-ui";
import AiToolEvidencePanel from "@/components/ai/AiToolEvidencePanel.vue";
import type { ChatMessage } from "@/types/aiView";
import {
  isBusinessCardSection,
  parseAssistantContent,
  priorityLabel,
  priorityType,
  renderInlineMarkdown,
  statusLabel
} from "@/utils/aiPresentation";

defineProps<{
  message: ChatMessage;
  messageIndex: number;
}>();
</script>

<style scoped>
.nd-ai-rich :deep(strong) {
  color: var(--text-primary);
  font-weight: 700;
}

.nd-ai-rich :deep(code) {
  border: 1px solid var(--border);
  border-radius: 4px;
  background: var(--surface);
  padding: 1px 5px;
  color: var(--text-primary);
  font-size: 0.85em;
}

.nd-ai-rich :deep(.nd-ai-number) {
  border-radius: 4px;
  background: rgba(14, 165, 233, 0.12);
  padding: 1px 4px;
  color: #0369a1;
  font-weight: 700;
}

.nd-ai-rich :deep(.nd-ai-risk-high) {
  border-radius: 4px;
  background: rgba(239, 68, 68, 0.12);
  padding: 1px 4px;
  color: #b91c1c;
  font-weight: 700;
}

.nd-ai-rich :deep(.nd-ai-risk-medium) {
  border-radius: 4px;
  background: rgba(245, 158, 11, 0.14);
  padding: 1px 4px;
  color: #92400e;
  font-weight: 700;
}

.nd-ai-action-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  padding: 8px 10px;
}
</style>
