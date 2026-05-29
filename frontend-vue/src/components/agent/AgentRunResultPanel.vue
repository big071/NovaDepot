<template>
  <article class="nd-table-shell">
    <div class="nd-table-head">
      <div>
        <h3 class="nd-section-title">执行过程</h3>
        <p class="nd-section-subtitle">步骤数：{{ currentSteps.length }}</p>
      </div>
    </div>
    <div class="nd-table-body">
      <n-data-table :columns="stepColumns" :data="currentSteps" :bordered="false" :max-height="320" />
    </div>
  </article>

  <article class="nd-table-shell">
    <div class="nd-table-head">
      <div>
        <h3 class="nd-section-title">执行结果</h3>
        <p class="nd-section-subtitle">状态：{{ currentRun?.status || '-' }}</p>
      </div>
    </div>
    <div class="nd-table-body space-y-3">
      <n-descriptions bordered :column="2" label-placement="left" size="small">
        <n-descriptions-item label="任务">{{ currentRun?.taskName || '-' }}</n-descriptions-item>
        <n-descriptions-item label="Run ID">{{ currentRun?.id || '-' }}</n-descriptions-item>
        <n-descriptions-item label="开始时间">{{ currentRun?.startedAt || '-' }}</n-descriptions-item>
        <n-descriptions-item label="结束时间">{{ currentRun?.finishedAt || '-' }}</n-descriptions-item>
      </n-descriptions>

      <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
        <article class="rounded-xl border border-border bg-bg/50 p-3" v-for="card in resultSummaryCards" :key="card.title">
          <p class="text-xs text-text-secondary">{{ card.title }}</p>
          <p class="mt-1 text-sm font-semibold">{{ card.value }}</p>
        </article>
      </div>

      <div class="grid gap-3 lg:grid-cols-2" v-if="basisList.length || actionList.length">
        <article class="rounded-xl border border-border bg-bg/40 p-3" v-if="basisList.length">
          <p class="text-sm font-medium">执行依据</p>
          <ul class="mt-2 space-y-1 text-xs text-text-secondary">
            <li v-for="item in basisList" :key="item">{{ item }}</li>
          </ul>
        </article>
        <article class="rounded-xl border border-border bg-bg/40 p-3" v-if="actionList.length">
          <p class="text-sm font-medium">建议动作</p>
          <ul class="mt-2 space-y-1 text-xs text-text-secondary">
            <li v-for="item in actionList" :key="item">{{ item }}</li>
          </ul>
        </article>
      </div>

      <article class="rounded-xl border border-border bg-bg/40 p-3">
        <p class="text-sm font-medium">结果明细</p>
        <n-data-table class="mt-2" :columns="resultColumns" :data="resultRows" :bordered="false" :max-height="320" />
        <n-empty v-if="resultRows.length === 0" class="nd-empty-shell mt-2" description="当前任务暂无可视化明细，建议查看技术详情。" />
      </article>

      <n-collapse>
        <n-collapse-item title="技术详情 / 调试信息（原始 JSON）" name="raw-json">
          <pre class="rounded-xl border border-border bg-bg/50 p-3 text-xs leading-5">{{ prettyJson(currentRun?.result || {}) }}</pre>
        </n-collapse-item>
      </n-collapse>
    </div>
  </article>
</template>

<script setup lang="ts">
import { NCollapse, NCollapseItem, NDataTable, NDescriptions, NDescriptionsItem, NEmpty } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import type { AgentRunDetail, AgentStepItem } from "@/services/agent";

defineProps<{
  currentRun: AgentRunDetail | null;
  currentSteps: AgentStepItem[];
  stepColumns: DataTableColumns<AgentStepItem>;
  resultSummaryCards: Array<{ title: string; value: string }>;
  basisList: string[];
  actionList: string[];
  resultColumns: DataTableColumns<Record<string, unknown>>;
  resultRows: Array<Record<string, unknown>>;
  prettyJson: (value: unknown) => string;
}>();
</script>
