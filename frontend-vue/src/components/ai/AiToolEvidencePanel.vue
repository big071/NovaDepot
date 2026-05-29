<template>
  <n-collapse v-if="toolCalls?.length" class="nd-tool-evidence" :default-expanded-names="[`tools-${messageIndex}`]">
    <n-collapse-item title="工具调用依据" :name="`tools-${messageIndex}`">
      <div class="space-y-2">
        <article
          v-for="(tool, toolIndex) in toolCalls"
          :key="`${tool.displayName || tool.toolName}-${toolIndex}-${tool.status}`"
          class="rounded-lg border border-border bg-bg/60 p-2 text-xs text-text-secondary"
        >
          <div class="flex items-center justify-between gap-2">
            <p class="font-medium text-text-primary">{{ tool.displayName || toolBusinessLabel(tool.toolName) }}</p>
            <n-tag size="small" :bordered="false" :type="toolStatusType(tool)">
              {{ toolStatusLabel(tool) }}
            </n-tag>
          </div>
          <p v-if="tool.argumentsSummary" class="mt-1">条件：{{ friendlyArguments(tool.argumentsSummary) }}</p>
          <p v-if="tool.summary" class="mt-1">{{ tool.summary }}</p>
          <div v-if="tool.sources?.length" class="mt-2 flex flex-wrap gap-1">
            <n-tag
              v-for="source in tool.sources.slice(0, 3)"
              :key="String(source.sourceId ?? source.bizNo ?? source.name)"
              size="small"
              :bordered="false"
              type="info"
            >
              {{ sourceLabel(source) }}
            </n-tag>
          </div>
        </article>
      </div>
    </n-collapse-item>
  </n-collapse>
</template>

<script setup lang="ts">
import { NCollapse, NCollapseItem, NTag } from "naive-ui";
import type { ToolCallMessage } from "@/types/aiView";
import { friendlyArguments, sourceLabel, toolBusinessLabel, toolStatusLabel, toolStatusType } from "@/utils/aiPresentation";

defineProps<{
  toolCalls?: ToolCallMessage[];
  messageIndex: number;
}>();
</script>

<style scoped>
.nd-tool-evidence :deep(.n-collapse-item__header-main) {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
