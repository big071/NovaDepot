<template>
  <article class="nd-table-shell">
    <div class="nd-table-head">
      <div>
        <h3 class="nd-section-title">任务列表</h3>
        <p class="nd-section-subtitle">只展示与业务直接相关的任务</p>
      </div>
    </div>
    <div class="nd-table-body space-y-3">
      <div class="grid gap-3 md:grid-cols-3">
        <button
          v-for="task in tasks"
          :key="task.taskCode"
          class="rounded-xl border p-3 text-left transition"
          :class="selectedTaskCode === task.taskCode ? 'border-primary bg-primary/10' : 'border-border bg-bg/50 hover:border-primary/50'"
          @click="$emit('select-task', task.taskCode)"
        >
          <p class="text-sm font-semibold">{{ displayTaskName(task) }}</p>
          <p class="mt-1 text-xs text-text-secondary">{{ displayTaskDescription(task) }}</p>
          <p class="mt-2 text-[11px] text-text-secondary">任务编码：{{ task.taskCode }}</p>
        </button>
      </div>

      <article v-if="selectedTask" class="rounded-xl border border-border bg-bg/50 p-3 text-xs text-text-secondary">
        <p class="font-medium text-text-primary">执行前说明</p>
        <p class="mt-1">这个任务会做什么：{{ displayTaskIntro(selectedTask) }}</p>
        <p class="mt-1">会读取哪些数据：{{ (selectedTask.readData ?? []).join("、") || "系统默认业务数据" }}</p>
        <p class="mt-1">输出什么结果：{{ selectedTask.output || "结构化任务结果、风险与建议" }}</p>
      </article>

      <div v-if="selectedTaskParams.length > 0" class="space-y-3 rounded-xl border border-border bg-bg/40 p-3">
        <p class="text-sm font-medium">任务参数（按业务含义填写）</p>
        <div class="grid gap-3 md:grid-cols-2">
          <article v-for="param in selectedTaskParams" :key="param.key" class="rounded-lg border border-border bg-surface p-3">
            <p class="text-sm font-medium">{{ param.label }}</p>
            <p class="mt-1 text-xs text-text-secondary">{{ param.description }}</p>
            <n-input-number
              v-if="param.type === 'number'"
              class="mt-2 w-full"
              :value="toNumber(taskForm[param.key])"
              :min="0"
              :show-button="false"
              :placeholder="param.placeholder || '请输入数值'"
              @update:value="(value) => $emit('update-task-form', param.key, value)"
            />
            <n-input
              v-else
              class="mt-2"
              :value="toText(taskForm[param.key])"
              :placeholder="param.placeholder || (param.type === 'date' ? 'YYYY-MM-DD' : '请输入内容')"
              @update:value="(value) => $emit('update-task-form', param.key, value)"
            />
          </article>
        </div>
      </div>

      <n-empty v-else-if="selectedTaskCode" class="nd-empty-shell" description="当前任务无需额外参数，可直接执行。" />
      <n-empty v-else class="nd-empty-shell" description="请先选择任务，参数区会按任务自动变化。" />

      <div class="flex justify-end">
        <n-button class="nd-soft-focus" type="primary" :loading="executing" :disabled="!selectedTaskCode || !canExecute" @click="$emit('execute')">
          {{ canExecute ? "执行任务" : "当前账号仅可查看历史" }}
        </n-button>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { NButton, NEmpty, NInput, NInputNumber } from "naive-ui";
import type { AgentTaskItem } from "@/services/agent";

export interface AgentTaskParamView {
  key: string;
  label: string;
  description: string;
  type: "number" | "text" | "date";
  defaultValue?: string | number;
  placeholder?: string;
}

defineProps<{
  tasks: AgentTaskItem[];
  selectedTaskCode: string;
  selectedTask: AgentTaskItem | null;
  selectedTaskParams: AgentTaskParamView[];
  taskForm: Record<string, string | number>;
  executing: boolean;
  canExecute: boolean;
  displayTaskName: (task: AgentTaskItem) => string;
  displayTaskDescription: (task: AgentTaskItem) => string;
  displayTaskIntro: (task: AgentTaskItem) => string;
  toNumber: (value: string | number | undefined) => number;
  toText: (value: string | number | undefined) => string;
}>();

defineEmits<{
  "select-task": [taskCode: string];
  "update-task-form": [key: string, value: string | number | null];
  execute: [];
}>();
</script>
