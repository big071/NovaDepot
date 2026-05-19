<template>
  <section class="nd-workbench-dual">
    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h2 class="nd-section-title">AI 会话</h2>
          <p class="nd-section-subtitle">共 {{ conversations.length }} 个会话</p>
        </div>
        <div class="flex gap-2">
          <n-button class="nd-soft-focus" size="small" @click="createConversation">新建</n-button>
          <n-button class="nd-soft-focus" size="small" :loading="loadingConversations"
            @click="loadConversations">刷新</n-button>
        </div>
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
        <button v-for="item in conversations" :key="item.conversationNo" class="nd-chat-list-item"
          :class="activeConversationNo === item.conversationNo ? 'border-primary bg-primary/10' : 'border-border hover:border-primary/40 hover:bg-bg'"
          @click="selectConversation(item.conversationNo)">
          <div class="flex items-center justify-between gap-2">
            <p class="font-medium">{{ item.conversationNo }}</p>
            <n-tag size="small" :bordered="false" :type="item.status === 'ARCHIVED' ? 'default' : 'info'">
              {{ item.status === 'ARCHIVED' ? '已归档' : item.provider }}
            </n-tag>
          </div>
          <p class="mt-1 text-xs text-text-secondary">{{ sceneLabelMap[item.scene] || item.scene }} · {{ item.lastActiveAt || item.startedAt }}</p>
        </button>
      </div>
    </article>

    <article class="nd-table-shell">
      <header class="nd-table-head">
        <div>
          <h1 class="text-xl font-semibold tracking-tight">AI 助手工作台</h1>
          <p class="text-sm text-text-secondary">AI 智能助手 · DeepSeek 驱动</p>
        </div>
        <div class="flex items-center gap-2">
          <span class="nd-pill">会话：{{ activeConversationNo || "未选择" }}</span>
          <n-button v-if="activeConversation && activeConversation.status !== 'ARCHIVED'" size="small" class="nd-soft-focus"
            @click="archiveActiveConversation">归档</n-button>
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
        <article v-if="lastKnowledgeRefs.length || lastKnowledgeNotice"
          class="mb-3 rounded-xl border border-border bg-bg/50 p-3 text-xs text-text-secondary">
          <p class="font-medium text-text-primary">知识引用来源</p>
          <div v-if="lastKnowledgeRefs.length" class="mt-2 flex flex-wrap gap-2">
            <n-tag v-for="ref in lastKnowledgeRefs" :key="`${ref.type}-${ref.code || ref.title}`" :bordered="false"
              type="info">
              {{ ref.type }}：{{ ref.title }} / {{ ref.scene || '通用' }}
            </n-tag>
          </div>
          <p v-else class="mt-1">{{ lastKnowledgeNotice }}</p>
        </article>
        <div class="mb-3 flex flex-wrap gap-2">
          <n-button v-for="q in recommendedQuestions" :key="q" class="nd-soft-focus" size="small"
            @click="askRecommended(q)">
            {{ q }}
          </n-button>
        </div>
        <n-alert v-if="lastSuccessText" type="success" :show-icon="false" class="nd-state-alert mb-3">{{ lastSuccessText
          }}</n-alert>
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
            <article v-for="card in taskSummaryCards" :key="card.title"
              class="rounded-xl border border-border bg-surface p-3">
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
            <n-data-table class="mt-2" :columns="taskResultColumns" :data="taskResultRows" :bordered="false"
              :max-height="260" />
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
              <n-button v-for="q in recommendedQuestions.slice(0, 3)" :key="`inline-${q}`" size="small"
                @click="askRecommended(q)">
                {{ q }}
              </n-button>
            </div>
          </div>
          <div v-for="(item, index) in activeMessages" :key="index"
            class="max-w-[85%] rounded-2xl px-3 py-2 text-sm leading-6"
            :class="item.role === 'user' ? 'ml-auto bg-primary text-white shadow-sm' : 'border border-border bg-surface text-text-primary'">
            <p v-if="item.role === 'user'">{{ item.content }}</p>
            <div v-else class="nd-ai-rich space-y-3">
              <section v-for="section in parseAssistantContent(item.content)" :key="section.title || section.content.join('|')"
                class="rounded-lg border border-border bg-bg/40 p-3">
                <div v-if="section.title" class="mb-2 flex items-center justify-between gap-2">
                  <h3 class="text-sm font-semibold text-text-primary">{{ section.title }}</h3>
                  <n-tag v-if="section.badge" size="small" :bordered="false" :type="section.badgeType">{{ section.badge }}</n-tag>
                </div>
                <ul v-if="section.items.length" class="space-y-2">
                  <li v-for="itemText in section.items" :key="itemText" class="leading-6" v-html="renderInlineMarkdown(itemText)"></li>
                </ul>
                <p v-for="paragraph in section.content" :key="paragraph" class="leading-6" v-html="renderInlineMarkdown(paragraph)"></p>
              </section>

              <n-collapse v-if="item.toolCalls?.length" class="nd-tool-evidence">
                <n-collapse-item title="工具调用依据" :name="`tools-${index}`">
                  <div class="space-y-2">
                    <article v-for="(tool, toolIndex) in item.toolCalls" :key="`${tool.displayName || tool.toolName}-${toolIndex}-${tool.status}`"
                      class="rounded-lg border border-border bg-bg/60 p-2 text-xs text-text-secondary">
                      <div class="flex items-center justify-between gap-2">
                        <p class="font-medium text-text-primary">{{ tool.displayName || toolBusinessLabel(tool.toolName) }}</p>
                        <n-tag size="small" :bordered="false" :type="toolStatusType(tool)">
                          {{ toolStatusLabel(tool) }}
                        </n-tag>
                      </div>
                      <p v-if="tool.argumentsSummary" class="mt-1">条件：{{ friendlyArguments(tool.argumentsSummary) }}</p>
                      <p v-if="tool.summary" class="mt-1">{{ tool.summary }}</p>
                      <div v-if="tool.sources?.length" class="mt-2 flex flex-wrap gap-1">
                        <n-tag v-for="source in tool.sources.slice(0, 3)" :key="String(source.sourceId ?? source.bizNo ?? source.name)"
                          size="small" :bordered="false" type="info">
                          {{ sourceLabel(source) }}
                        </n-tag>
                      </div>
                    </article>
                  </div>
                </n-collapse-item>
              </n-collapse>
            </div>
            <div v-if="item.role === 'assistant' && item.validationWarnings?.length" class="mt-2 space-y-1 text-xs text-warning">
              <p v-for="warning in item.validationWarnings" :key="warning">{{ warning }}</p>
            </div>
            <p v-if="item.role === 'assistant' && item.toolLimitReached" class="mt-2 text-xs text-warning">
              已达到本轮最多 5 次工具调用限制。
            </p>
            <p v-if="item.role === 'assistant' && item.status && item.status !== 'COMPLETED'"
              class="mt-1 text-xs text-text-secondary">
              {{ statusLabel(item.status) }}
            </p>
          </div>
        </div>

        <div class="mt-3 space-y-2">
          <n-alert v-if="errorText" class="nd-state-alert" type="error" :show-icon="false">{{ errorText }}</n-alert>
          <div class="nd-chat-composer">
            <div class="flex gap-2">
              <n-input class="nd-soft-focus" v-model:value="inputText" placeholder="输入问题并发送"
                @keyup.enter="sendMessage" />
              <n-button v-if="sending" class="nd-soft-focus" type="warning" @click="stopGeneration">停止</n-button>
              <n-button v-else class="nd-soft-focus" type="primary" :disabled="!canSend"
                @click="sendMessage">发送</n-button>
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
import { NAlert, NButton, NCollapse, NCollapseItem, NDataTable, NEmpty, NInput, NSelect, NTag, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import { aiApi, streamAiChat, type AiConversation, type AiMessage, type AiStreamEvent, type AiToolCallView } from "@/services/ai";
import type { KnowledgeRef } from "@/services/knowledge";

type ChatMessage = {
  role: "user" | "assistant";
  content: string;
  status?: "PENDING" | "STREAMING" | "COMPLETED" | "FAILED" | "STOPPED";
  toolCalls?: ToolCallMessage[];
  validationWarnings?: string[];
  toolLimitReached?: boolean;
};
type ToolCallMessage = AiToolCallView & { status?: "CALLING" | "SUCCESS" | "DENIED" | "EMPTY" | "FAILED" };
type SummaryCard = { title: string; value: string };
type RenderedSection = {
  title: string;
  content: string[];
  items: string[];
  badge?: string;
  badgeType?: "default" | "error" | "info" | "success" | "warning";
};

const uiMessage = useMessage();
const router = useRouter();
const loadingConversations = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const errorText = ref("");
const lastSuccessText = ref("");
const streamStatusText = ref("");
const scene = ref("enterprise");
const inputText = ref("");
const conversations = ref<AiConversation[]>([]);
const activeConversationNo = ref<string | null>(null);
const messageMap = ref<Record<string, ChatMessage[]>>({});
const abortController = ref<AbortController | null>(null);
const activeRequestId = ref("");
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
const activeConversation = computed(() => conversations.value.find((item) => item.conversationNo === activeConversationNo.value) ?? null);
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
const canSend = computed(() => !sending.value && !loadingMessages.value && Boolean(inputText.value.trim()) && activeConversation.value?.status !== "ARCHIVED");

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

function parseAssistantContent(content: string): RenderedSection[] {
  const cleaned = (content || "").replace(/\r/g, "").trim();
  if (!cleaned) {
    return [{ title: "生成中", content: ["正在整理业务回答..."], items: [], badge: "AI", badgeType: "info" }];
  }
  const knownTitles = ["当前结论", "主要风险", "建议动作", "数据依据", "下一步", "下一步可执行操作"];
  const sections: RenderedSection[] = [];
  let current: RenderedSection = { title: "当前结论", content: [], items: [], badge: "结论", badgeType: "success" };

  for (const rawLine of cleaned.split("\n")) {
    const line = rawLine.trim();
    if (!line) continue;
    const normalized = line.replace(/^#{1,6}\s*/, "").replace(/^\*\*(.+)\*\*$/, "$1").replace(/[:：]$/, "");
    const matchedTitle = knownTitles.find((title) => normalized === title || normalized.startsWith(title));
    if (matchedTitle) {
      if (current.content.length || current.items.length) sections.push(current);
      current = {
        title: matchedTitle === "下一步" ? "下一步可执行操作" : matchedTitle,
        content: [],
        items: [],
        ...sectionBadge(matchedTitle)
      };
      continue;
    }

    const bullet = line.match(/^[-*]\s+(.+)$/) || line.match(/^\d+[.)、]\s+(.+)$/);
    if (bullet) {
      current.items.push(cleanToolNames(bullet[1]));
    } else {
      splitLongParagraph(cleanToolNames(line)).forEach((paragraph) => current.content.push(paragraph));
    }
  }
  if (current.content.length || current.items.length) sections.push(current);
  return sections.length ? sections : [{ title: "当前结论", content: splitLongParagraph(cleanToolNames(cleaned)), items: [], badge: "结论", badgeType: "success" }];
}

function sectionBadge(title: string): Pick<RenderedSection, "badge" | "badgeType"> {
  if (title.includes("风险")) return { badge: "风险", badgeType: "warning" };
  if (title.includes("动作") || title.includes("下一步")) return { badge: "行动", badgeType: "info" };
  if (title.includes("依据")) return { badge: "依据", badgeType: "default" };
  return { badge: "结论", badgeType: "success" };
}

function splitLongParagraph(text: string) {
  if (text.length <= 120) return [text];
  return text.split(/(?<=[。！？；])/).map((item) => item.trim()).filter(Boolean);
}

function renderInlineMarkdown(text: string) {
  const escaped = escapeHtml(text);
  return escaped
    .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/(高优先级|高风险|失败|异常|超时|未查询到相关数据)/g, "<span class=\"nd-ai-risk-high\">$1</span>")
    .replace(/(中优先级|中风险|待审核|待处理|未关闭)/g, "<span class=\"nd-ai-risk-medium\">$1</span>")
    .replace(/(\d+(?:\.\d+)?\s*(?:件|条|个|元|天|小时|%|SKU|单)?)/g, "<span class=\"nd-ai-number\">$1</span>");
}

function escapeHtml(text: string) {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function cleanToolNames(text: string) {
  return text
    .replace(/\bquery_inventory\b/g, "库存查询")
    .replace(/\bquery_purchase\b/g, "采购查询")
    .replace(/\bquery_outbound\b/g, "出库查询")
    .replace(/\bquery_tickets\b/g, "工单查询")
    .replace(/\bquery_inbound\b/g, "入库查询")
    .replace(/\bquery_sale\b/g, "销售查询")
    .replace(/\bget_daily_report\b/g, "运营日报查询")
    .replace(/\bget_inventory_stats\b/g, "库存统计查询");
}

function toolBusinessLabel(toolName?: string) {
  if (!toolName) return "业务查询";
  const map: Record<string, string> = {
    query_inventory: "库存查询",
    query_purchase: "采购查询",
    query_outbound: "出库查询",
    query_tickets: "工单查询",
    query_inbound: "入库查询",
    query_sale: "销售查询",
    query_product: "商品查询",
    query_partner: "往来单位查询",
    get_daily_report: "运营日报查询",
    get_inventory_stats: "库存统计查询"
  };
  return map[toolName] || "业务查询";
}

function friendlyArguments(argumentsSummary: string) {
  return cleanToolNames(argumentsSummary)
    .replace(/lowStock/g, "低库存")
    .replace(/limit/g, "条数")
    .replace(/status/g, "状态")
    .replace(/orderNo/g, "单号")
    .replace(/dateFrom/g, "开始日期")
    .replace(/dateTo/g, "结束日期");
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
    const current = conversations.value.find((item) => item.conversationNo === activeConversationNo.value);
    if (!activeConversationNo.value || current?.status === "ARCHIVED") {
      const next = conversations.value.find((item) => item.status !== "ARCHIVED") ?? conversations.value[0];
      if (next) {
        await selectConversation(next.conversationNo);
      }
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
    content: msg.content,
    status: msg.status ?? "COMPLETED"
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

async function createConversation() {
  errorText.value = "";
  try {
    const created = await aiApi.createConversation(scene.value);
    await loadConversations();
    activeConversationNo.value = created.conversationNo;
    messageMap.value[created.conversationNo] = [];
    lastSuccessText.value = "新会话已创建";
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "新建会话失败";
    uiMessage.error(errorText.value);
  }
}

async function archiveActiveConversation() {
  const current = activeConversation.value;
  if (!current) return;
  errorText.value = "";
  try {
    await aiApi.archiveConversation(current.id);
    await loadConversations();
    lastSuccessText.value = "会话已归档";
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "归档会话失败";
    uiMessage.error(errorText.value);
  }
}

async function sendMessage() {
  if (!inputText.value.trim()) {
    uiMessage.warning("请输入问题后再发送");
    return;
  }

  errorText.value = "";
  streamStatusText.value = "";
  lastSuccessText.value = "";
  const content = inputText.value.trim();
  const normalizedMessage = normalizeMessageForAgent(content);
  inputText.value = "";

  const optimisticConversationNo = activeConversationNo.value ?? "__pending__";
  if (!messageMap.value[optimisticConversationNo]) {
    messageMap.value[optimisticConversationNo] = [];
  }
  messageMap.value[optimisticConversationNo].push({ role: "user", content });
  messageMap.value[optimisticConversationNo].push({ role: "assistant", content: "", status: "STREAMING", toolCalls: [], validationWarnings: [] });

  sending.value = true;
  const requestId = crypto.randomUUID();
  activeRequestId.value = requestId;
  abortController.value = new AbortController();
  try {
    await streamAiChat({
      scene: scene.value,
      message: normalizedMessage,
      conversationNo: activeConversationNo.value ?? undefined
    }, requestId, abortController.value.signal, (event) => handleStreamEvent(event, optimisticConversationNo));
    lastSuccessText.value = `消息发送成功：${new Date().toLocaleString("zh-CN", { hour12: false })}`;
    await loadConversations();
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      markLastAssistant(optimisticConversationNo, "STOPPED");
      streamStatusText.value = "已停止生成，保留当前已输出内容。";
    } else {
      const conversationNo = activeConversationNo.value ?? optimisticConversationNo;
      const message = formatAiFailure(error);
      markLastAssistant(conversationNo, "FAILED");
      setLastAssistantContent(conversationNo, message);
      errorText.value = message;
      streamStatusText.value = "";
      uiMessage.error("DeepSeek 调用失败");
    }
  } finally {
    sending.value = false;
    abortController.value = null;
    activeRequestId.value = "";
  }
}

function handleStreamEvent(event: AiStreamEvent, optimisticConversationNo: string) {
  if (event.event === "meta") {
    const conversationNo = String(event.data.conversationNo ?? optimisticConversationNo);
    if (conversationNo !== optimisticConversationNo && messageMap.value[optimisticConversationNo]) {
      messageMap.value[conversationNo] = messageMap.value[optimisticConversationNo];
      delete messageMap.value[optimisticConversationNo];
    }
    activeConversationNo.value = conversationNo;
    return;
  }
  const conversationNo = activeConversationNo.value ?? optimisticConversationNo;
  if (event.event === "token") {
    appendAssistantToken(conversationNo, event.data.content ?? "");
  } else if (event.event === "status") {
    streamStatusText.value = String(event.data.message ?? statusLabel(String(event.data.status ?? "STREAMING")));
    if (event.data.status === "STOPPED") markLastAssistant(conversationNo, "STOPPED");
  } else if (event.event === "tool_start") {
    upsertToolCall(conversationNo, { ...event.data, status: "CALLING" });
  } else if (event.event === "tool_result") {
    const status = event.data.permissionResult === "DENIED"
      ? "DENIED"
      : event.data.success === false
        ? "FAILED"
        : event.data.empty
          ? "EMPTY"
          : "SUCCESS";
    upsertToolCall(conversationNo, { ...event.data, status });
  } else if (event.event === "tool_error") {
    upsertToolCall(conversationNo, { ...event.data, summary: event.data.message || event.data.summary, status: "FAILED" });
  } else if (event.event === "validation_warning") {
    appendValidationWarning(conversationNo, event.data.message || "回答已进行工具结果一致性校验。");
  } else if (event.event === "tool_limit") {
    markToolLimit(conversationNo, event.data.message);
  } else if (event.event === "done") {
    markLastAssistant(conversationNo, String(event.data.status ?? "COMPLETED") as ChatMessage["status"]);
    const doneToolCalls = event.data.toolCalls;
    if (Array.isArray(doneToolCalls)) {
      setToolCalls(conversationNo, doneToolCalls as ChatMessage["toolCalls"]);
    }
    const warnings = event.data.validationWarnings;
    if (Array.isArray(warnings)) {
      setValidationWarnings(conversationNo, warnings.map((item) => String(item)));
    }
    if (event.data.toolLimitReached) {
      markToolLimit(conversationNo);
    }
    if (event.data.fallbackFrom) {
      streamStatusText.value = `已按显式 fallback 配置从 ${event.data.fallbackFrom} 降级完成。`;
    }
  } else if (event.event === "error") {
    throw new Error(formatAiFailure(event.data));
  }
}

async function sendNonStreamingFallback(normalizedMessage: string, optimisticConversationNo: string) {
  try {
    const resp = await aiApi.chat({
      scene: scene.value,
      message: normalizedMessage,
      conversationNo: activeConversationNo.value ?? undefined
    });
    activeConversationNo.value = resp.conversationNo;
    if (optimisticConversationNo !== resp.conversationNo && messageMap.value[optimisticConversationNo]) {
      delete messageMap.value[optimisticConversationNo];
    }
    await loadConversations();
    await loadConversationMessages(resp.conversationNo);
    const list = messageMap.value[resp.conversationNo] ?? [];
    const last = [...list].reverse().find((item) => item.role === "assistant");
    if (last) {
      last.toolCalls = resp.toolCalls ?? [];
      last.validationWarnings = resp.validationWarnings ?? [];
      last.toolLimitReached = Boolean(resp.toolLimitReached);
    }
    lastKnowledgeRefs.value = resp.knowledgeRefs || [];
    lastKnowledgeNotice.value = resp.knowledgeFallbackNotice || "";
  } catch (error) {
    markLastAssistant(activeConversationNo.value ?? optimisticConversationNo, "FAILED");
    errorText.value = error instanceof Error ? error.message : "AI 回复失败";
    uiMessage.error(errorText.value);
  }
}

async function stopGeneration() {
  if (!activeRequestId.value) return;
  abortController.value?.abort();
  try {
    await aiApi.stopStream(activeRequestId.value);
  } catch {
    // Local abort already stopped the UI stream; backend cleanup is best-effort.
  }
}

function appendAssistantToken(conversationNo: string, token: string) {
  const list = messageMap.value[conversationNo] ?? [];
  const last = [...list].reverse().find((item) => item.role === "assistant");
  if (last) {
    last.content += token;
    last.status = "STREAMING";
  }
}

function markLastAssistant(conversationNo: string, status: ChatMessage["status"]) {
  const list = messageMap.value[conversationNo] ?? [];
  const last = [...list].reverse().find((item) => item.role === "assistant");
  if (last) {
    last.status = status;
  }
}

function setLastAssistantContent(conversationNo: string, content: string) {
  const list = messageMap.value[conversationNo] ?? [];
  const last = [...list].reverse().find((item) => item.role === "assistant");
  if (last && !last.content) {
    last.content = content;
  }
}

function formatAiFailure(error: unknown) {
  const message = error instanceof Error
    ? error.message
    : typeof error === "object" && error !== null && "message" in error
      ? String((error as { message?: unknown }).message ?? "")
      : "";
  if (message.includes("DeepSeek 调用失败")) {
    return message;
  }
  return [
    "DeepSeek 调用失败，请检查：",
    "1. API Key 是否正确",
    "2. AI_DEEPSEEK_ENABLED 是否为 true",
    "3. AI_PROVIDER 是否为 deepseek-chat",
    "4. 网络是否能访问 DeepSeek",
    "5. 账户额度是否充足",
    "6. 模型名称是否正确"
  ].join("\n");
}

function lastAssistant(conversationNo: string) {
  const list = messageMap.value[conversationNo] ?? [];
  return [...list].reverse().find((item) => item.role === "assistant");
}

function upsertToolCall(conversationNo: string, tool: Partial<ToolCallMessage>) {
  const last = lastAssistant(conversationNo);
  if (!last || !tool.toolName) return;
  const existing = last.toolCalls ?? [];
  const index = existing.findIndex((item) => item.toolName === tool.toolName);
  if (index >= 0) {
    existing[index] = { ...existing[index], ...tool } as ToolCallMessage;
  } else {
    existing.push(tool as ToolCallMessage);
  }
  last.toolCalls = existing;
}

function setToolCalls(conversationNo: string, toolCalls: ChatMessage["toolCalls"]) {
  const last = lastAssistant(conversationNo);
  if (last) last.toolCalls = toolCalls ?? [];
}

function appendValidationWarning(conversationNo: string, warning: string) {
  const last = lastAssistant(conversationNo);
  if (!last) return;
  last.validationWarnings = [...(last.validationWarnings ?? []), warning];
}

function setValidationWarnings(conversationNo: string, warnings: string[]) {
  const last = lastAssistant(conversationNo);
  if (last) last.validationWarnings = warnings;
}

function markToolLimit(conversationNo: string, message?: string) {
  const last = lastAssistant(conversationNo);
  if (!last) return;
  last.toolLimitReached = true;
  if (message) appendValidationWarning(conversationNo, message);
}

function toolStatusLabel(tool: ToolCallMessage) {
  if (tool.status === "CALLING") return "调用中";
  if (tool.permissionResult === "DENIED" || tool.status === "DENIED") return "无权限";
  if (tool.status === "FAILED" || tool.success === false) return "失败";
  if (tool.empty || tool.status === "EMPTY") return "无结果";
  return "成功";
}

function toolStatusType(tool: ToolCallMessage) {
  if (tool.status === "CALLING") return "info";
  if (tool.permissionResult === "DENIED" || tool.status === "DENIED") return "warning";
  if (tool.status === "FAILED" || tool.success === false) return "error";
  if (tool.empty || tool.status === "EMPTY") return "default";
  return "success";
}

function sourceLabel(source: Record<string, unknown>) {
  const main = source.bizNo ?? source.name ?? source.productName ?? source.sourceId ?? "-";
  const status = source.status ? ` / ${source.status}` : "";
  const qty = source.quantity ?? source.availableQty;
  return `${main}${status}${qty === undefined ? "" : ` / ${qty}`}`;
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: "等待生成",
    STREAMING: "生成中...",
    COMPLETED: "已完成",
    FAILED: "生成失败",
    STOPPED: "已停止"
  };
  return map[status] || status;
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

.nd-tool-evidence :deep(.n-collapse-item__header-main) {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
