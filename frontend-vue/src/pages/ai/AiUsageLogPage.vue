<template>
  <section class="nd-workbench-single">
    <header class="nd-table-head">
      <div>
        <h1 class="text-xl font-semibold tracking-tight">AI 用量与配置</h1>
        <p class="text-sm text-text-secondary">查看 AI Provider、fallback 状态、Token、延迟和失败记录</p>
      </div>
      <n-button class="nd-soft-focus" size="small" :loading="loading || loadingConfig" @click="refreshAll">刷新</n-button>
    </header>

    <div class="nd-table-body">
      <section class="mb-4 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <article class="rounded-lg border border-border bg-surface p-3">
          <p class="text-xs text-text-secondary">当前 Provider</p>
          <p class="mt-1 text-base font-semibold text-text-primary">{{ config?.defaultProvider || "-" }}</p>
          <p class="mt-1 text-xs text-text-secondary">{{ config?.activeModel || "-" }}</p>
        </article>
        <article class="rounded-lg border border-border bg-surface p-3">
          <p class="text-xs text-text-secondary">DeepSeek 状态</p>
          <n-tag class="mt-2" :bordered="false" :type="config?.deepseekEnabled ? 'success' : 'error'">
            {{ config?.deepseekEnabled ? "已启用" : "未启用" }}
          </n-tag>
          <p class="mt-2 text-xs text-text-secondary">{{ config?.deepseekBaseUrl || "-" }}</p>
        </article>
        <article class="rounded-lg border border-border bg-surface p-3">
          <p class="text-xs text-text-secondary">Fallback 状态</p>
          <n-tag class="mt-2" :bordered="false" :type="config?.fallbackEnabled ? 'warning' : 'default'">
            {{ config?.fallbackEnabled ? "允许降级到规则引擎" : "禁用降级，失败直接报错" }}
          </n-tag>
        </article>
        <article class="rounded-lg border border-border bg-surface p-3">
          <p class="text-xs text-text-secondary">工具调用</p>
          <n-tag class="mt-2" :bordered="false" :type="config?.toolsEnabled ? 'success' : 'default'">
            {{ config?.toolsEnabled ? "已启用" : "未启用" }}
          </n-tag>
          <p class="mt-2 text-xs text-text-secondary">Key：{{ config?.deepseekApiKeyMasked || "-" }}</p>
        </article>
      </section>

      <n-collapse v-if="config?.systemPromptPreview" class="mb-4">
        <n-collapse-item title="System Prompt 预览" name="prompt">
          <pre class="whitespace-pre-wrap rounded-lg border border-border bg-bg p-3 text-xs leading-5 text-text-secondary">{{ config.systemPromptPreview }}</pre>
        </n-collapse-item>
      </n-collapse>

      <div class="mb-4 flex flex-wrap items-center gap-3">
        <n-select v-model:value="providerFilter" class="w-44 nd-soft-focus" size="small" clearable
          placeholder="Provider" :options="providerOptions" />
        <n-input-number v-model:value="limit" class="w-36 nd-soft-focus" size="small" :min="10" :max="200" />
        <n-tag :bordered="false" type="info">总数：{{ filteredLogs.length }}</n-tag>
        <n-tag :bordered="false" type="success">成功：{{ successCount }}</n-tag>
        <n-tag :bordered="false" type="error">失败/降级：{{ failCount }}</n-tag>
        <div class="ml-auto text-xs text-text-secondary">
          总 Token：{{ totalTokens }} | 平均延迟：{{ avgLatency }}ms
        </div>
      </div>

      <n-empty v-if="!loading && filteredLogs.length === 0" description="暂无 AI 用量记录">
        <template #extra>
          <p class="text-xs text-text-secondary">使用 AI 助手后，用量、失败和降级记录会显示在这里。</p>
        </template>
      </n-empty>

      <n-data-table v-else :columns="columns" :data="filteredLogs" :bordered="false" :max-height="520"
        :loading="loading" :pagination="pagination" size="small" class="nd-chat-list-item" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref, watch } from "vue";
import { NButton, NCollapse, NCollapseItem, NDataTable, NEmpty, NInputNumber, NSelect, NTag, useMessage } from "naive-ui";
import type { DataTableColumns, PaginationProps } from "naive-ui";
import { aiApi, type AiConfig, type AiUsageLog } from "@/services/ai";

const uiMessage = useMessage();
const loading = ref(false);
const loadingConfig = ref(false);
const logs = ref<AiUsageLog[]>([]);
const config = ref<AiConfig | null>(null);
const providerFilter = ref<string | null>(null);
const limit = ref(100);

const providerOptions = [
  { label: "DeepSeek Chat", value: "deepseek-chat" },
  { label: "DeepSeek Reasoner", value: "deepseek-reasoner" },
  { label: "RuleProvider", value: "rule" },
  { label: "MockProvider", value: "mock" }
];

const filteredLogs = computed(() => {
  if (!providerFilter.value) return logs.value;
  return logs.value.filter((item) => item.provider === providerFilter.value);
});

const successCount = computed(() => filteredLogs.value.filter((item) => item.success).length);
const failCount = computed(() => filteredLogs.value.filter((item) => !item.success).length);
const totalTokens = computed(() => filteredLogs.value.reduce((sum, item) => sum + (item.totalTokens || 0), 0));
const avgLatency = computed(() => {
  if (filteredLogs.value.length === 0) return 0;
  const sum = filteredLogs.value.reduce((acc, item) => acc + (item.latencyMs || 0), 0);
  return Math.round(sum / filteredLogs.value.length);
});

const pagination: PaginationProps = {
  pageSize: 10,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
};

const columns: DataTableColumns<AiUsageLog> = [
  { title: "ID", key: "id", width: 90 },
  { title: "会话 ID", key: "conversationId", width: 100 },
  { title: "Provider", key: "provider", width: 150 },
  { title: "Model", key: "model", width: 170 },
  { title: "场景", key: "scene", width: 100 },
  { title: "输入 Token", key: "promptTokens", width: 100 },
  { title: "输出 Token", key: "completionTokens", width: 100 },
  { title: "总 Token", key: "totalTokens", width: 90 },
  { title: "延迟(ms)", key: "latencyMs", width: 100 },
  {
    title: "状态",
    key: "success",
    width: 100,
    render: (row) =>
      h(NTag, { bordered: false, type: row.success ? "success" : "error", size: "small" }, () =>
        row.success ? "成功" : "失败"
      )
  },
  { title: "错误码", key: "errorCode", width: 150, render: (row) => row.errorCode || "-" },
  {
    title: "失败/降级原因",
    key: "errorMessage",
    width: 240,
    ellipsis: { tooltip: true },
    render: (row) => row.errorMessage || "-"
  },
  {
    title: "时间",
    key: "createdAt",
    width: 180,
    render: (row) => {
      if (!row.createdAt) return "-";
      return new Date(row.createdAt).toLocaleString("zh-CN", { hour12: false });
    }
  }
];

async function loadConfig() {
  loadingConfig.value = true;
  try {
    config.value = await aiApi.config();
  } catch (error) {
    uiMessage.error(error instanceof Error ? error.message : "AI 配置加载失败");
  } finally {
    loadingConfig.value = false;
  }
}

async function loadLogs() {
  loading.value = true;
  try {
    logs.value = await aiApi.usageLogs(undefined, limit.value);
  } catch (error) {
    uiMessage.error(error instanceof Error ? error.message : "AI 用量日志加载失败");
  } finally {
    loading.value = false;
  }
}

async function refreshAll() {
  await Promise.all([loadConfig(), loadLogs()]);
}

watch(limit, loadLogs);
onMounted(refreshAll);
</script>
