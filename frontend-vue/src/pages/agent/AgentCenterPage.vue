<template>
  <section class="space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Agent</p>
          <h1 class="nd-page-title">任务中心</h1>
          <p class="nd-page-subtitle">先选任务，再填写业务参数。默认先看业务化结果，原始 JSON 只放在技术详情里。</p>
        </div>
        <n-button class="nd-soft-focus" :loading="loadingTasks" @click="loadTasks">刷新任务</n-button>
      </div>
    </header>

    <n-alert class="nd-state-alert" type="info" :show-icon="false">
      新手引导：1. 选择任务 2. 阅读“执行前说明” 3. 按中文提示填写参数 4. 执行后先看摘要卡片和结果表格。
    </n-alert>
    <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">{{ errorText }}</n-alert>

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
            @click="selectTask(task.taskCode)"
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
                @update:value="(value) => updateTaskFormValue(param.key, value)"
              />
              <n-input
                v-else
                class="mt-2"
                :value="toText(taskForm[param.key])"
                :placeholder="param.placeholder || (param.type === 'date' ? 'YYYY-MM-DD' : '请输入内容')"
                @update:value="(value) => updateTaskFormValue(param.key, value)"
              />
            </article>
          </div>
        </div>

        <n-empty v-else-if="selectedTaskCode" class="nd-empty-shell" description="当前任务无需额外参数，可直接执行。" />
        <n-empty v-else class="nd-empty-shell" description="请先选择任务，参数区会按任务自动变化。" />

        <div class="flex justify-end">
          <n-button class="nd-soft-focus" type="primary" :loading="executing" :disabled="!selectedTaskCode || !canExecute" @click="onExecute">
            {{ canExecute ? "执行任务" : "当前账号仅可查看历史" }}
          </n-button>
        </div>
      </div>
    </article>

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

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h3 class="nd-section-title">历史记录</h3>
          <p class="nd-section-subtitle">共 {{ total }} 条</p>
        </div>
        <n-button class="nd-soft-focus" :loading="loadingRuns" @click="loadRuns">刷新历史</n-button>
      </div>
      <div class="nd-table-body">
        <n-data-table :columns="runColumns" :data="runs" :loading="loadingRuns" :bordered="false" />
        <div class="mt-4 flex justify-end" v-if="total > 0">
          <n-pagination
            :page="pageNo"
            :page-size="pageSize"
            :item-count="total"
            :page-sizes="[10, 20, 50]"
            show-size-picker
            @update:page="onPageChange"
            @update:page-size="onPageSizeChange"
          />
        </div>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import {
  NAlert,
  NButton,
  NCollapse,
  NCollapseItem,
  NDataTable,
  NDescriptions,
  NDescriptionsItem,
  NEmpty,
  NInput,
  NInputNumber,
  NPagination,
  useMessage
} from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { agentApi, type AgentRunDetail, type AgentRunListItem, type AgentStepItem, type AgentTaskItem } from "@/services/agent";
import { useAuthStore } from "@/stores/auth";

const message = useMessage();
const route = useRoute();
const authStore = useAuthStore();

const loadingTasks = ref(false);
const loadingRuns = ref(false);
const executing = ref(false);
const errorText = ref("");

const tasks = ref<AgentTaskItem[]>([]);
const selectedTaskCode = ref("");
const taskForm = reactive<Record<string, string | number>>({});

const currentRun = ref<AgentRunDetail | null>(null);
const runs = ref<AgentRunListItem[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(10);

const canExecute = computed(() => authStore.hasPermission("AGENT_TASK_EXECUTE"));
const selectedTask = computed(() => tasks.value.find((item) => item.taskCode === selectedTaskCode.value) ?? null);
const currentSteps = computed(() => currentRun.value?.steps ?? []);
const resultView = computed<Record<string, unknown>>(() => (currentRun.value?.result?.resultView as Record<string, unknown> | undefined) ?? {});

const taskMetaMap: Record<string, {
  taskName: string;
  description: string;
  intro: string;
  paramMeta: Record<string, { label: string; description: string; placeholder?: string }>;
}> = {
  LOW_STOCK_ANALYSIS: {
    taskName: "低库存分析",
    description: "分析低库存风险并按优先级输出处理清单。",
    intro: "自动分析低库存记录，结合近期出库趋势给出风险排序。",
    paramMeta: {
      limit: { label: "建议输出条数", description: "需要展示多少条重点风险商品，建议 3-10 条。", placeholder: "例如 5" },
      recentDays: { label: "近几天出库趋势", description: "读取近 N 天出库趋势辅助判断风险。", placeholder: "例如 7" }
    }
  },
  REPLENISH_SUGGESTION: {
    taskName: "补货建议",
    description: "根据低库存与阈值输出补货建议。",
    intro: "自动识别低库存商品并给出建议补货量和原因。",
    paramMeta: {
      limit: { label: "建议输出条数", description: "需要输出多少条补货建议。", placeholder: "例如 5" },
      lowStockThreshold: { label: "低库存阈值", description: "未配置安全库存时使用的兜底阈值。", placeholder: "例如 10" }
    }
  },
  ANOMALY_PATROL: {
    taskName: "异常巡检",
    description: "巡检负库存、低库存和未完成单据异常。",
    intro: "扫描库存与单据异常并输出可处理清单。",
    paramMeta: {
      staleDays: { label: "巡检窗口天数", description: "检查近 N 天未完成单据。", placeholder: "例如 3" },
      lowStockThreshold: { label: "低库存阈值", description: "低库存判定阈值。", placeholder: "例如 10" }
    }
  },
  DAILY_OPERATIONS_REPORT: {
    taskName: "运营日报",
    description: "汇总当日入库、出库、工单和库存风险。",
    intro: "自动生成指定日期的运营日报摘要。",
    paramMeta: {
      date: { label: "日报日期", description: "不填默认今天，格式 YYYY-MM-DD。", placeholder: "例如 2026-04-23" }
    }
  },
  CS_TICKET_TRIAGE: {
    taskName: "工单处理建议",
    description: "输出工单分类、优先级和处理建议。",
    intro: "对待处理工单给出分类、优先级与回复建议。",
    paramMeta: {
      limit: { label: "建议输出条数", description: "返回多少条待处理工单建议。", placeholder: "例如 5" }
    }
  },
  SOP_RECOMMENDATION: {
    taskName: "SOP 建议",
    description: "按主题生成标准处理步骤。",
    intro: "根据业务主题生成可执行 SOP 步骤。",
    paramMeta: {
      topic: { label: "SOP 主题", description: "例如：物流催发、退换货、库存异常。", placeholder: "例如 物流催发" }
    }
  }
};

const selectedTaskParams = computed(() => {
  const task = selectedTask.value;
  if (!task?.params || task.params.length === 0) return [];
  return task.params.map((param) => {
    const meta = taskMetaMap[task.taskCode]?.paramMeta[param.key];
    return {
      ...param,
      label: meta?.label || param.label || param.key,
      description: meta?.description || param.description || "请按业务实际填写。",
      placeholder: meta?.placeholder
    };
  });
});

const stepColumns: DataTableColumns<AgentStepItem> = [
  { title: "#", key: "stepNo", width: 70 },
  { title: "阶段", key: "phase", width: 110 },
  { title: "步骤", key: "name", minWidth: 180 },
  { title: "状态", key: "status", width: 110 },
  { title: "耗时(ms)", key: "durationMs", width: 110 },
  { title: "说明", key: "detail", minWidth: 220 }
];

const runColumns: DataTableColumns<AgentRunListItem> = [
  { title: "ID", key: "id", width: 90 },
  { title: "任务", key: "taskName", minWidth: 180 },
  { title: "状态", key: "status", width: 110 },
  { title: "开始", key: "startedAt", minWidth: 180 },
  { title: "结束", key: "finishedAt", minWidth: 180 },
  {
    title: "操作",
    key: "actions",
    width: 100,
    render: (row) =>
      h(
        NButton,
        {
          class: "nd-soft-focus",
          size: "small",
          onClick: () => loadRunDetail(row.id)
        },
        { default: () => "详情" }
      )
  }
];

const resultSummaryCards = computed(() => {
  const run = currentRun.value;
  if (!run) return [];
  const cards = Array.isArray(resultView.value.summaryCards) ? resultView.value.summaryCards : [];
  if (cards.length > 0) {
    return cards.map((item) => ({
      title: String((item as Record<string, unknown>).title ?? "摘要"),
      value: formatCell((item as Record<string, unknown>).value)
    }));
  }
  const result = run.result ?? {};
  return [
    { title: "任务名称", value: run.taskName || "-" },
    { title: "结论摘要", value: formatCell(result.summary ?? result.reportText ?? "暂无摘要") },
    { title: "重点指标", value: summarizeMetrics(result.metrics) || "-" },
    { title: "建议动作", value: summarizeActions(result) || "-" }
  ];
});

const basisList = computed(() => {
  const raw = resultView.value.basisList;
  if (Array.isArray(raw)) return raw.map((item) => String(item));
  const businessBasis = currentRun.value?.result?.businessBasis;
  if (Array.isArray(businessBasis)) {
    return businessBasis.map((item) => {
      const ref = item as Record<string, unknown>;
      return `${ref.type || "知识"}：${ref.title || "-"} / ${ref.scene || "通用"} / ${ref.reason || "业务依据"}`;
    });
  }
  return [];
});

const actionList = computed(() => {
  const raw = resultView.value.recommendedActions;
  if (Array.isArray(raw)) return raw.map((item) => String(item));
  return [];
});

const resultRows = computed<Array<Record<string, unknown>>>(() => {
  const tables = Array.isArray(resultView.value.tables) ? resultView.value.tables : [];
  const firstTable = tables[0] as Record<string, unknown> | undefined;
  const rows = firstTable?.rows;
  if (Array.isArray(rows)) {
    return rows.map((item) => (typeof item === "object" && item !== null ? item as Record<string, unknown> : { value: item }));
  }
  const run = currentRun.value;
  if (!run) return [];
  return extractResultRows(run.taskCode, run.result ?? {});
});

const resultColumns = computed<DataTableColumns<Record<string, unknown>>>(() => {
  const tables = Array.isArray(resultView.value.tables) ? resultView.value.tables : [];
  const firstTable = tables[0] as Record<string, unknown> | undefined;
  const columns = firstTable?.columns;
  if (Array.isArray(columns) && columns.length > 0) {
    return columns.map((item) => {
      const column = item as Record<string, unknown>;
      const key = String(column.key ?? "value");
      return {
        title: String(column.label ?? key),
        key,
        minWidth: 120,
        render: (row: Record<string, unknown>) => formatCell(row[key])
      };
    });
  }
  const first = resultRows.value[0];
  if (!first) return [];
  return Object.keys(first).slice(0, 8).map((key) => ({
    title: resultFieldLabel(key),
    key,
    minWidth: 120,
    render: (row: Record<string, unknown>) => formatCell(row[key])
  }));
});

function prettyJson(value: unknown) {
  return JSON.stringify(value ?? {}, null, 2);
}

function resultFieldLabel(key: string) {
  const map: Record<string, string> = {
    productCode: "商品编码",
    productName: "商品名称",
    availableQty: "可用库存",
    currentQty: "当前库存",
    safetyStock: "安全库存",
    suggestReplenishQty: "建议补货量",
    reason: "原因",
    categorySuggestion: "分类建议",
    prioritySuggestion: "优先级建议",
    replySuggestion: "候选回复",
    sopSuggestion: "SOP 建议",
    faqHitSuggestion: "FAQ 命中",
    reportDate: "日期",
    reportText: "日报摘要",
    section: "模块",
    inbound: "入库",
    outbound: "出库",
    tickets: "工单",
    lowStock: "低库存",
    status: "状态",
    staleDays: "未完成天数",
    action: "建议动作",
    step: "步骤"
  };
  return map[key] || key;
}

function formatCell(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

function summarizeMetrics(metrics: unknown) {
  if (!metrics || typeof metrics !== "object") return "";
  const entries = Object.entries(metrics as Record<string, unknown>).slice(0, 3);
  return entries.map(([key, value]) => `${resultFieldLabel(key)}:${formatCell(value)}`).join("；");
}

function summarizeActions(result: Record<string, unknown>) {
  const rows = extractResultRows(String(result.taskCode ?? ""), result);
  if (rows.length === 0) return "";
  const sample = rows[0] as Record<string, unknown>;
  const name = formatCell(sample.productName ?? sample.ticketNo ?? sample.section ?? sample.topic ?? sample.step);
  const action = formatCell(sample.reason ?? sample.replySuggestion ?? sample.sopSuggestion ?? sample.action);
  return action === "-" ? name : `${name}：${action}`;
}

function extractResultRows(taskCode: string, result: Record<string, unknown>) {
  if (taskCode === "DAILY_OPERATIONS_REPORT") {
    const sections = (result.sections as Record<string, Record<string, unknown>> | undefined) ?? {};
    return Object.entries(sections).map(([key, value]) => ({ section: resultFieldLabel(key), ...value }));
  }
  if (taskCode === "SOP_RECOMMENDATION") {
    const steps = Array.isArray(result.steps) ? result.steps : [];
    return steps.map((step, index) => ({ step: `步骤 ${index + 1}`, action: formatCell(step) }));
  }
  const primaryKeys = ["suggestions", "analysisList", "topRiskItems", "anomalies", "items", "list"];
  for (const key of primaryKeys) {
    const candidate = result[key];
    if (Array.isArray(candidate)) {
      return candidate.map((item) => (typeof item === "object" && item !== null ? item as Record<string, unknown> : { value: item }));
    }
  }
  return [];
}

function displayTaskName(task: AgentTaskItem) {
  return taskMetaMap[task.taskCode]?.taskName || task.taskName;
}

function displayTaskDescription(task: AgentTaskItem) {
  return taskMetaMap[task.taskCode]?.description || task.description;
}

function displayTaskIntro(task: AgentTaskItem) {
  return taskMetaMap[task.taskCode]?.intro || task.intro || task.description;
}

function selectTask(taskCode: string) {
  selectedTaskCode.value = taskCode;
  const task = tasks.value.find((item) => item.taskCode === taskCode);
  if (!task?.params) {
    Object.keys(taskForm).forEach((key) => delete taskForm[key]);
    return;
  }
  const nextForm: Record<string, string | number> = {};
  task.params.forEach((param) => {
    if (typeof param.defaultValue === "number" || typeof param.defaultValue === "string") {
      nextForm[param.key] = param.defaultValue;
      return;
    }
    nextForm[param.key] = param.type === "number" ? 0 : "";
  });
  Object.keys(taskForm).forEach((key) => delete taskForm[key]);
  Object.assign(taskForm, nextForm);
}

function updateTaskFormValue(key: string, value: string | number | null) {
  taskForm[key] = value ?? "";
}

function toNumber(value: string | number | undefined) {
  if (typeof value === "number") return value;
  if (!value) return 0;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function toText(value: string | number | undefined) {
  if (value === undefined || value === null) return "";
  return String(value);
}

function buildTargetPayload() {
  const payload: Record<string, unknown> = {};
  const params = selectedTaskParams.value;
  params.forEach((param) => {
    const raw = taskForm[param.key];
    if (param.type === "number") {
      payload[param.key] = toNumber(raw as string | number | undefined);
      return;
    }
    const text = toText(raw as string | number | undefined).trim();
    if (text) {
      payload[param.key] = text;
    }
  });
  return payload;
}

async function loadTasks() {
  loadingTasks.value = true;
  errorText.value = "";
  try {
    tasks.value = await agentApi.listTasks();
    if (!selectedTaskCode.value && tasks.value.length > 0) {
      selectTask(tasks.value[0].taskCode);
    }
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "任务列表加载失败";
    message.error(errorText.value);
  } finally {
    loadingTasks.value = false;
  }
}

async function onExecute() {
  if (!selectedTaskCode.value || !canExecute.value) return;
  executing.value = true;
  errorText.value = "";
  try {
    const run = await agentApi.executeTask(selectedTaskCode.value, buildTargetPayload());
    currentRun.value = run;
    await loadRuns();
    message.success(`任务执行完成：${run.taskName}`);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "任务执行失败";
    message.error(errorText.value);
  } finally {
    executing.value = false;
  }
}

async function loadRuns() {
  loadingRuns.value = true;
  try {
    const resp = await agentApi.listRuns({ pageNo: pageNo.value, pageSize: pageSize.value });
    runs.value = resp.list ?? [];
    total.value = Number(resp.total ?? 0);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "历史记录加载失败");
  } finally {
    loadingRuns.value = false;
  }
}

async function loadRunDetail(id: string) {
  try {
    currentRun.value = await agentApi.getRunDetail(id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "执行详情加载失败");
  }
}

function onPageChange(nextPage: number) {
  pageNo.value = nextPage;
  loadRuns();
}

function onPageSizeChange(nextSize: number) {
  pageSize.value = nextSize;
  pageNo.value = 1;
  loadRuns();
}

onMounted(async () => {
  await loadTasks();
  await loadRuns();
  const runId = String(route.query.runId ?? "").trim();
  if (runId) {
    await loadRunDetail(runId);
  }
});
</script>
