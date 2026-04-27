<template>
  <section class="nd-workbench-dual">
    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h2 class="nd-section-title">AI 会话</h2>
          <p class="nd-section-subtitle">共 {{ conversations.length }} 个会话</p>
        </div>
        <n-button class="nd-soft-focus" size="small" :loading="loadingConversations" @click="loadConversations">刷新</n-button>
      </div>
      <div class="nd-table-body space-y-2">
        <n-empty v-if="!loadingConversations && conversations.length === 0" description="暂无会话，可点击推荐问题快速开始。">
          <template #extra>
            <div class="flex flex-wrap gap-2">
              <n-button v-for="q in recommendedQuestions.slice(0, 2)" :key="q" size="small" @click="askRecommended(q)">
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
          @click="selectConversation(item.conversationNo)"
        >
          <div class="flex items-center justify-between gap-2">
            <p class="font-medium">{{ item.conversationNo }}</p>
            <n-tag size="small" :bordered="false" type="info">{{ item.provider }}</n-tag>
          </div>
          <p class="mt-1 text-xs text-text-secondary">{{ sceneLabelMap[item.scene] || item.scene }}</p>
        </button>
      </div>
    </article>

    <article class="nd-table-shell">
      <header class="nd-table-head">
        <div>
          <h1 class="text-xl font-semibold tracking-tight">AI 助手工作台</h1>
          <p class="text-sm text-text-secondary">免费方案优先：RuleProvider / MockProvider</p>
        </div>
        <div class="flex items-center gap-2">
          <span class="nd-pill">会话：{{ activeConversationNo || "未选择" }}</span>
          <n-select v-model:value="scene" :options="sceneOptions" size="small" class="w-36 nd-soft-focus" />
        </div>
      </header>
      <div class="nd-table-body">
        <n-alert class="nd-state-alert mb-3" type="info" :show-icon="false">
          新手可先点击推荐问题。识别为任务型请求时，会自动调用 Agent，并返回结论摘要、执行依据和表格明细。
        </n-alert>
        <article class="mb-3 rounded-xl border border-border bg-bg/50 p-3 text-xs text-text-secondary">
          AI 建议依据说明：优先使用真实库存、库存流水、低库存阈值、单据与客服事实；若数据不足，会返回规则化建议与下一步动作。
        </article>
        <article v-if="lastKnowledgeRefs.length || lastKnowledgeNotice" class="mb-3 rounded-xl border border-border bg-bg/50 p-3 text-xs text-text-secondary">
          <p class="font-medium text-text-primary">知识引用来源</p>
          <div v-if="lastKnowledgeRefs.length" class="mt-2 flex flex-wrap gap-2">
            <n-tag v-for="ref in lastKnowledgeRefs" :key="`${ref.type}-${ref.code || ref.title}`" :bordered="false" type="info">
              {{ ref.type }}：{{ ref.title }} / {{ ref.scene || '通用' }}
            </n-tag>
          </div>
          <p v-else class="mt-1">{{ lastKnowledgeNotice }}</p>
        </article>
        <div class="mb-3 flex flex-wrap gap-2">
          <n-button v-for="q in recommendedQuestions" :key="q" class="nd-soft-focus" size="small" @click="askRecommended(q)">
            {{ q }}
          </n-button>
        </div>
        <n-alert v-if="lastSuccessText" type="success" :show-icon="false" class="nd-state-alert mb-3">{{ lastSuccessText }}</n-alert>

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

          <div class="mt-3 grid gap-3 lg:grid-cols-2" v-if="taskBasisList.length || taskActionList.length">
            <article class="rounded-xl border border-border bg-surface p-3" v-if="taskBasisList.length">
              <p class="text-sm font-medium text-text-primary">执行依据</p>
              <ul class="mt-2 space-y-1 text-xs text-text-secondary">
                <li v-for="item in taskBasisList" :key="item">{{ item }}</li>
              </ul>
            </article>
            <article class="rounded-xl border border-border bg-surface p-3" v-if="taskActionList.length">
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
            <n-button class="nd-soft-focus" size="small" type="primary" @click="openTaskDetail">查看任务详情</n-button>
            <n-button class="nd-soft-focus" size="small" @click="goAgentCenter">跳转 Agent Center</n-button>
          </div>
        </article>

        <div class="nd-chat-shell h-[460px] space-y-3 overflow-y-auto">
          <p v-if="loadingMessages" class="text-sm text-text-secondary">历史消息加载中...</p>
          <div v-else-if="activeMessages.length === 0" class="space-y-2 text-sm text-text-secondary">
            <p>当前会话暂无消息，可先使用推荐问题快速体验。</p>
            <div class="flex flex-wrap gap-2">
              <n-button v-for="q in recommendedQuestions.slice(0, 3)" :key="`inline-${q}`" size="small" @click="askRecommended(q)">
                {{ q }}
              </n-button>
            </div>
          </div>
          <div
            v-for="(item, index) in activeMessages"
            :key="index"
            class="max-w-[85%] rounded-2xl px-3 py-2 text-sm leading-6"
            :class="item.role === 'user' ? 'ml-auto bg-primary text-white shadow-sm' : 'border border-border bg-surface text-text-primary'"
          >
            <p>{{ item.content }}</p>
          </div>
        </div>

        <div class="mt-3 space-y-2">
          <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">{{ errorText }}</n-alert>
          <div class="nd-chat-composer">
            <div class="flex gap-2">
              <n-input class="nd-soft-focus" v-model:value="inputText" placeholder="输入问题并发送" @keyup.enter="sendMessage" />
              <n-button class="nd-soft-focus" type="primary" :loading="sending" :disabled="!canSend" @click="sendMessage">发送</n-button>
            </div>
          </div>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { NAlert, NButton, NDataTable, NEmpty, NInput, NSelect, NTag, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { aiApi, type AiConversation, type AiMessage } from "@/services/ai";
import type { KnowledgeRef } from "@/services/knowledge";

type ChatMessage = { role: "user" | "assistant"; content: string };
type SummaryCard = { title: string; value: string };

const uiMessage = useMessage();
const router = useRouter();
const loadingConversations = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const errorText = ref("");
const lastSuccessText = ref("");
const scene = ref("enterprise");
const inputText = ref("");
const conversations = ref<AiConversation[]>([]);
const activeConversationNo = ref<string | null>(null);
const messageMap = ref<Record<string, ChatMessage[]>>({});
const taskRunInfo = ref<{
  id?: string;
  taskCode?: string;
  taskName?: string;
  executionBasis?: Record<string, unknown>;
  executionResult?: Record<string, unknown>;
  resultView?: Record<string, unknown>;
} | null>(null);
const lastKnowledgeRefs = ref<KnowledgeRef[]>([]);
const lastKnowledgeNotice = ref("");

const sceneLabelMap: Record<string, string> = {
  enterprise: "企业助手",
  warehouse: "仓储助手",
  sop: "SOP 助手"
};

const sceneOptions = [
  { label: "企业助手", value: "enterprise" },
  { label: "仓储助手", value: "warehouse" },
  { label: "SOP 助手", value: "sop" }
];

const activeMessages = computed(() => {
  if (!activeConversationNo.value) return [];
  return messageMap.value[activeConversationNo.value] ?? [];
});
const recommendedQuestions = computed(() => {
  if (scene.value === "warehouse") {
    return [
      "优先补货顺序是什么？",
      "请分析过去7天出库趋势并给出补货建议。",
      "请按风险高到低列出需要关注的SKU。"
    ];
  }
  if (scene.value === "sop") {
    return [
      "给我一个物流催发 SOP。",
      "请给客服一段可直接发送给客户的回复模板。",
      "请总结当前工单处理中的高风险点。"
    ];
  }
  return [
    "今天最需要处理什么？",
    "请总结今日经营情况：入库、出库、低库存风险。",
    "请给管理层一份今日运营摘要，包含下一步建议。"
  ];
});
const canSend = computed(() => !sending.value && !loadingMessages.value && Boolean(inputText.value.trim()));

const taskView = computed<Record<string, unknown>>(() => {
  const current = taskRunInfo.value;
  if (!current) return {};
  return (current.resultView ?? current.executionResult?.resultView ?? {}) as Record<string, unknown>;
});

const taskSummaryCards = computed<SummaryCard[]>(() => {
  const current = taskRunInfo.value;
  if (!current) return [];
  const view = taskView.value;
  const cards = Array.isArray(view.summaryCards) ? view.summaryCards : [];
  if (cards.length > 0) {
    return cards.map((item) => ({
      title: String((item as Record<string, unknown>).title ?? "摘要"),
      value: formatCell((item as Record<string, unknown>).value)
    }));
  }
  const result = current.executionResult ?? {};
  return [
    { title: "任务名称", value: current.taskName || "-" },
    { title: "结论摘要", value: formatCell(result.summary ?? result.reportText ?? "已完成") },
    { title: "执行依据", value: summarizeObject(current.executionBasis) || "-" },
    { title: "建议动作", value: summarizeActions(result) || "查看执行详情" }
  ];
});

const taskBasisList = computed(() => {
  const raw = taskView.value.basisList;
  if (Array.isArray(raw)) return raw.map((item) => String(item));
  const fallback = summarizeObject(taskRunInfo.value?.executionBasis ?? {});
  return fallback ? [fallback] : [];
});

const taskActionList = computed(() => {
  const raw = taskView.value.recommendedActions;
  if (Array.isArray(raw)) return raw.map((item) => String(item));
  const fallback = summarizeActions(taskRunInfo.value?.executionResult ?? {});
  return fallback ? [fallback] : [];
});

const taskResultRows = computed<Array<Record<string, unknown>>>(() => {
  const view = taskView.value;
  const tables = Array.isArray(view.tables) ? view.tables : [];
  const firstTable = tables[0] as Record<string, unknown> | undefined;
  const rows = firstTable?.rows;
  if (Array.isArray(rows)) {
    return rows.map((item) => (typeof item === "object" && item !== null ? item as Record<string, unknown> : { value: item }));
  }
  const result = taskRunInfo.value?.executionResult ?? {};
  return extractRows(String(taskRunInfo.value?.taskCode ?? ""), result);
});

const taskResultColumns = computed<DataTableColumns<Record<string, unknown>>>(() => {
  const view = taskView.value;
  const tables = Array.isArray(view.tables) ? view.tables : [];
  const firstTable = tables[0] as Record<string, unknown> | undefined;
  const columns = firstTable?.columns;
  if (Array.isArray(columns) && columns.length > 0) {
    return columns.map((item) => {
      const column = item as Record<string, unknown>;
      const key = String(column.key ?? "value");
      return {
        title: String(column.label ?? key),
        key,
        minWidth: 140,
        render: (row: Record<string, unknown>) => formatCell(row[key])
      };
    });
  }
  const first = taskResultRows.value[0];
  if (!first) return [];
  return Object.keys(first).slice(0, 8).map((key) => ({
    title: resultFieldLabel(key),
    key,
    minWidth: 140,
    render: (row: Record<string, unknown>) => formatCell(row[key])
  }));
});

function summarizeObject(value: unknown) {
  if (!value || typeof value !== "object") return "";
  return Object.entries(value as Record<string, unknown>)
    .slice(0, 3)
    .map(([key, val]) => `${resultFieldLabel(key)}：${formatCell(val)}`)
    .join("；");
}

function summarizeActions(result: Record<string, unknown>) {
  const rows = extractRows(String(result.taskCode ?? ""), result);
  if (rows.length === 0) return "";
  const sample = rows[0] as Record<string, unknown>;
  return formatCell(sample.reason ?? sample.replySuggestion ?? sample.sopSuggestion ?? sample.action ?? sample.productName ?? sample.section);
}

function extractRows(taskCode: string, result: Record<string, unknown>) {
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

async function openTaskDetail() {
  if (!taskRunInfo.value?.id) return;
  await router.push({ path: "/agent/center", query: { runId: taskRunInfo.value.id } });
}

async function goAgentCenter() {
  await router.push("/agent/center");
}

async function askRecommended(question: string) {
  inputText.value = question;
  await sendMessage();
}

async function loadConversations() {
  loadingConversations.value = true;
  errorText.value = "";
  try {
    conversations.value = await aiApi.conversations();
    if (!activeConversationNo.value && conversations.value.length > 0) {
      await selectConversation(conversations.value[0].conversationNo);
    }
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "会话列表加载失败";
    uiMessage.error(errorText.value);
  } finally {
    loadingConversations.value = false;
  }
}

function toChatMessage(msg: AiMessage): ChatMessage {
  return {
    role: msg.role === "USER" ? "user" : "assistant",
    content: msg.content
  };
}

async function loadConversationMessages(conversationNo: string) {
  loadingMessages.value = true;
  errorText.value = "";
  try {
    const data = await aiApi.messagesByNo(conversationNo);
    messageMap.value[conversationNo] = data.map(toChatMessage);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "会话消息加载失败";
    uiMessage.error(errorText.value);
  } finally {
    loadingMessages.value = false;
  }
}

async function selectConversation(conversationNo: string) {
  activeConversationNo.value = conversationNo;
  errorText.value = "";
  await loadConversationMessages(conversationNo);
}

async function sendMessage() {
  if (!inputText.value.trim()) {
    uiMessage.warning("请输入问题后再发送");
    return;
  }

  errorText.value = "";
  const content = inputText.value.trim();
  const normalizedMessage = normalizeMessageForAgent(content);
  inputText.value = "";

  const optimisticConversationNo = activeConversationNo.value ?? "__pending__";
  if (!messageMap.value[optimisticConversationNo]) {
    messageMap.value[optimisticConversationNo] = [];
  }
  messageMap.value[optimisticConversationNo].push({ role: "user", content });

  sending.value = true;
  try {
    const resp = await aiApi.chat({
      scene: scene.value,
      message: normalizedMessage,
      conversationNo: activeConversationNo.value ?? undefined,
      providerHint: "rule"
    });

    activeConversationNo.value = resp.conversationNo;
    if (optimisticConversationNo !== resp.conversationNo && messageMap.value[optimisticConversationNo]) {
      delete messageMap.value[optimisticConversationNo];
    }
    await loadConversations();
    await loadConversationMessages(resp.conversationNo);
    if (resp.taskRouted && resp.taskRun) {
      taskRunInfo.value = {
        id: String(resp.taskRun.id ?? ""),
        taskCode: resp.taskCode,
        taskName: resp.taskName,
        executionBasis: resp.executionBasis,
        executionResult: resp.executionResult,
        resultView: resp.resultView
      };
    } else {
      taskRunInfo.value = null;
    }
    lastKnowledgeRefs.value = resp.knowledgeRefs || [];
    lastKnowledgeNotice.value = resp.knowledgeFallbackNotice || "";
    lastSuccessText.value = `消息发送成功：${new Date().toLocaleString("zh-CN", { hour12: false })}`;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "AI 回复失败";
    uiMessage.error(errorText.value);
  } finally {
    sending.value = false;
  }
}

function normalizeMessageForAgent(content: string) {
  const text = content.trim();
  if (["今天最需要处理什么", "今天先处理什么", "优先处理什么", "今日最该处理什么"].includes(text)) {
    return "生成今日运营日报，并告诉我今天最需要优先处理的事项";
  }
  return text;
}

onMounted(loadConversations);
</script>
