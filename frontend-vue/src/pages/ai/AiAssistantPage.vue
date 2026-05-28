<template>
  <section class="nd-workbench-dual">
    <AiConversationSidebar
      :conversations="conversations"
      :loading="loadingConversations"
      :active-conversation-no="activeConversationNo"
      :recommended-questions="recommendedQuestions"
      :scene-label-map="sceneLabelMap"
      @create="createConversation"
      @refresh="loadConversations"
      @select="selectConversation"
      @ask="askRecommended"
    />

    <article class="nd-table-shell">
      <AiProviderStatusBar
        v-model:scene="scene"
        :active-conversation="activeConversation"
        :active-conversation-no="activeConversationNo"
        :scene-options="sceneOptions"
        :ai-config-warning="aiConfigWarning"
        :last-knowledge-refs="lastKnowledgeRefs"
        :last-knowledge-notice="lastKnowledgeNotice"
        :recommended-questions="recommendedQuestions"
        :last-success-text="lastSuccessText"
        :stream-status-text="streamStatusText"
        :task-run-info="taskRunInfo"
        :task-summary-cards="taskSummaryCards"
        :task-basis-list="taskBasisList"
        :task-action-list="taskActionList"
        :task-result-columns="taskResultColumns"
        :task-result-rows="taskResultRows"
        @archive="archiveActiveConversation"
        @ask="askRecommended"
        @open-task-detail="openTaskDetail"
        @go-agent-center="goAgentCenter"
      >
        <AiMessageList
          :loading="loadingMessages"
          :messages="activeMessages"
          :recommended-questions="recommendedQuestions"
          @ask="askRecommended"
        />
        <AiInputBar
          v-model="inputText"
          :sending="sending"
          :can-send="canSend"
          :disabled="Boolean(aiConfigWarning)"
          :error-text="errorText"
          @send="sendMessage"
          @stop="stopGeneration"
        />
      </AiProviderStatusBar>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import AiConversationSidebar from "@/components/ai/AiConversationSidebar.vue";
import AiInputBar from "@/components/ai/AiInputBar.vue";
import AiMessageList from "@/components/ai/AiMessageList.vue";
import AiProviderStatusBar from "@/components/ai/AiProviderStatusBar.vue";
import { aiApi, streamAiChat, type AiConfig, type AiConversation, type AiMessage, type AiStreamEvent } from "@/services/ai";
import type { KnowledgeRef } from "@/services/knowledge";
import type { AiTaskRunInfo, ChatMessage, SummaryCard, ToolCallMessage } from "@/types/aiView";
import {
  extractRows,
  formatAiFailure,
  formatCell,
  normalizeMessageForAgent,
  resultFieldLabel,
  statusLabel,
  summarizeActions,
  summarizeObject
} from "@/utils/aiPresentation";

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
const aiConfig = ref<AiConfig | null>(null);
const taskRunInfo = ref<AiTaskRunInfo | null>(null);
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
const aiConfigWarning = computed(() => {
  const config = aiConfig.value;
  if (!config || config.defaultProvider !== "deepseek-chat") return "";
  if (!config.deepseekEnabled) {
    return "DeepSeek 未启用：请设置 AI_DEEPSEEK_ENABLED=true 并重启后端。";
  }
  if (!config.deepseekApiKeyMasked || config.deepseekApiKeyMasked === "***") {
    return "DeepSeek API Key 未配置：请在本地 .env 设置 AI_DEEPSEEK_API_KEY 后重启后端。";
  }
  return "";
});
const canSend = computed(() => !sending.value && !loadingMessages.value && !aiConfigWarning.value
  && Boolean(inputText.value.trim()) && activeConversation.value?.status !== "ARCHIVED");

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

async function loadAiConfig() {
  try {
    aiConfig.value = await aiApi.config();
  } catch {
    aiConfig.value = null;
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
  if (aiConfigWarning.value) {
    errorText.value = aiConfigWarning.value;
    uiMessage.warning(aiConfigWarning.value);
    return;
  }
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

onMounted(async () => {
  await loadAiConfig();
  await loadConversations();
});
</script>
