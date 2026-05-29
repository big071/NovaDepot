<template>
  <header class="nd-table-head">
    <div>
      <h1 class="text-xl font-semibold tracking-tight">AI 助手工作台</h1>
      <p class="text-sm text-text-secondary">AI 智能助手 · DeepSeek 驱动</p>
    </div>
    <div class="flex items-center gap-2">
      <span class="nd-pill">会话：{{ activeConversationNo || "未选择" }}</span>
      <n-button
        v-if="activeConversation && activeConversation.status !== 'ARCHIVED'"
        size="small"
        class="nd-soft-focus"
        @click="$emit('archive')"
      >
        归档
      </n-button>
      <n-select :value="scene" :options="sceneOptions" size="small" class="w-36 nd-soft-focus" @update:value="$emit('update:scene', $event)" />
    </div>
  </header>
  <div class="nd-table-body">
    <n-alert v-if="aiConfigWarning" class="nd-state-alert mb-3" type="warning" :show-icon="false">
      {{ aiConfigWarning }}
    </n-alert>
    <n-alert class="nd-state-alert mb-3" type="info" :show-icon="false">
      新手可先点击推荐问题。识别为任务型请求时，会自动调用 Agent，并返回结论摘要、执行依据和表格明细。
    </n-alert>
    <article class="mb-3 rounded-xl border border-border bg-bg/50 p-3 text-xs text-text-secondary">
      AI 建议依据说明：优先使用真实库存、库存流水、低库存阈值、单据与客服事实；若数据不足，会返回规则化建议与下一步动作。
    </article>
    <article
      v-if="lastKnowledgeRefs.length || lastKnowledgeNotice"
      class="mb-3 rounded-xl border border-border bg-bg/50 p-3 text-xs text-text-secondary"
    >
      <p class="font-medium text-text-primary">知识引用来源</p>
      <div v-if="lastKnowledgeRefs.length" class="mt-2 flex flex-wrap gap-2">
        <n-tag v-for="ref in lastKnowledgeRefs" :key="`${ref.type}-${ref.code || ref.title}`" :bordered="false" type="info">
          {{ ref.type }}：{{ ref.title }} / {{ ref.scene || '通用' }}
        </n-tag>
      </div>
      <p v-else class="mt-1">{{ lastKnowledgeNotice }}</p>
    </article>
    <div class="mb-3 flex flex-wrap gap-2">
      <n-button v-for="q in recommendedQuestions" :key="q" class="nd-soft-focus" size="small" @click="$emit('ask', q)">
        {{ q }}
      </n-button>
    </div>
    <n-alert v-if="lastSuccessText" type="success" :show-icon="false" class="nd-state-alert mb-3">{{ lastSuccessText }}</n-alert>
    <n-alert v-if="streamStatusText" type="info" :show-icon="false" class="nd-state-alert mb-3">{{ streamStatusText }}</n-alert>

    <article v-if="taskRunInfo" class="mb-3 rounded-xl border border-border bg-bg/50 p-4">
      <div class="flex items-center justify-between gap-2">
        <div>
          <p class="text-sm font-medium text-text-primary">任务执行结果：{{ taskRunInfo.taskName }}</p>
          <p class="mt-1 text-xs text-text-secondary">识别为任务型请求后，系统已自动调用 Agent 执行。</p>
        </div>
        <n-tag :bordered="false" size="small" type="success">Run {{ taskRunInfo.id || "-" }}</n-tag>
      </div>

      <div class="mt-3 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <article v-for="card in taskSummaryCards" :key="card.title" class="rounded-xl border border-border bg-surface p-3">
          <p class="text-xs text-text-secondary">{{ card.title }}</p>
          <p class="mt-1 text-sm font-semibold text-text-primary">{{ card.value }}</p>
        </article>
      </div>

      <div v-if="taskBasisList.length || taskActionList.length" class="mt-3 grid gap-3 lg:grid-cols-2">
        <article v-if="taskBasisList.length" class="rounded-xl border border-border bg-surface p-3">
          <p class="text-sm font-medium text-text-primary">执行依据</p>
          <ul class="mt-2 space-y-1 text-xs text-text-secondary">
            <li v-for="item in taskBasisList" :key="item">{{ item }}</li>
          </ul>
        </article>
        <article v-if="taskActionList.length" class="rounded-xl border border-border bg-surface p-3">
          <p class="text-sm font-medium text-text-primary">建议动作</p>
          <ul class="mt-2 space-y-1 text-xs text-text-secondary">
            <li v-for="item in taskActionList" :key="item">{{ item }}</li>
          </ul>
        </article>
      </div>

      <article class="mt-3 rounded-xl border border-border bg-surface p-3">
        <p class="text-sm font-medium text-text-primary">结果明细</p>
        <n-data-table class="mt-2" :columns="taskResultColumns" :data="taskResultRows" :bordered="false" :max-height="260" />
        <n-empty v-if="taskResultRows.length === 0" class="mt-2" description="当前任务暂无可视化明细，可查看执行详情。" />
      </article>

      <div class="mt-3 flex flex-wrap gap-2">
        <n-button class="nd-soft-focus" size="small" type="primary" @click="$emit('openTaskDetail')">查看任务详情</n-button>
        <n-button class="nd-soft-focus" size="small" @click="$emit('goAgentCenter')">跳转 Agent Center</n-button>
      </div>
    </article>

    <slot />
  </div>
</template>

<script setup lang="ts">
import { NAlert, NButton, NDataTable, NEmpty, NSelect, NTag } from "naive-ui";
import type { AiProviderStatusContext } from "@/types/aiView";

defineProps<AiProviderStatusContext>();

defineEmits<{
  "update:scene": [scene: string];
  archive: [];
  ask: [question: string];
  openTaskDetail: [];
  goAgentCenter: [];
}>();
</script>
