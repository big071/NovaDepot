<template>
  <section class="space-y-4">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Customer Service</p>
          <h1 class="nd-page-title">客服工单闭环工作台</h1>
          <p class="nd-page-subtitle">可查看 AI 自动回复、人工接管、责任人变更、状态变更和处理备注历史，支持按审计记录回查。</p>
        </div>
        <n-button class="nd-soft-focus" :loading="loadingSessions" @click="loadSessions">刷新</n-button>
      </div>
    </header>

    <n-alert v-if="errorText" type="error" :show-icon="false">{{ errorText }}</n-alert>
    <n-alert v-else-if="successText" type="success" :show-icon="false">{{ successText }}</n-alert>

    <div class="grid gap-4 xl:grid-cols-[320px,1fr]">
      <CustomerSessionList
        :sessions="sessions"
        :active-session-id="activeSessionId"
        :loading="loadingSessions"
        @select-session="selectSession"
      />

      <article class="space-y-4">
        <div class="grid gap-3 md:grid-cols-4">
          <article class="rounded-xl border border-border bg-bg/50 p-3">
            <p class="text-xs text-text-secondary">当前会话</p>
            <p class="mt-1 text-sm font-semibold">{{ activeSession?.sessionNo || "-" }}</p>
          </article>
          <article class="rounded-xl border border-border bg-bg/50 p-3">
            <p class="text-xs text-text-secondary">当前责任人</p>
            <p class="mt-1 text-sm font-semibold">{{ activeSession?.assignedUserId || "-" }}</p>
          </article>
          <article class="rounded-xl border border-border bg-bg/50 p-3">
            <p class="text-xs text-text-secondary">AI自动回复</p>
            <p class="mt-1 text-sm font-semibold">{{ autoReplyEnabled ? "开启" : "关闭" }}</p>
          </article>
          <article class="rounded-xl border border-border bg-bg/50 p-3">
            <p class="text-xs text-text-secondary">下一步建议</p>
            <p class="mt-1 text-sm font-semibold">{{ aiSuggestion?.recommendedAction || "-" }}</p>
          </article>
        </div>

        <CustomerSuggestionPanel
          :active-session-id="activeSessionId"
          :loading-suggestion="loadingSuggestion"
          :suggestion-first="suggestionFirst"
          :auto-reply-enabled="autoReplyEnabled"
          :ai-suggestion="aiSuggestion"
          :messages="messages"
          :input-text="inputText"
          :sending="sending"
          @load-suggestion="loadSuggestion"
          @use-suggestion="inputText = suggestionFirst"
          @transfer-human="transferHuman"
          @toggle-auto-reply="autoReplyEnabled = !autoReplyEnabled"
          @update-input="inputText = $event"
          @send-agent-message="sendAgentMessage"
          @send-customer-message="sendCustomerMessage"
        />

        <CustomerTicketList
          :active-session-id="activeSessionId"
          :tickets="tickets"
          :ticket-status-draft="ticketStatusDraft"
          :ticket-owner-draft="ticketOwnerDraft"
          :ticket-remark-draft="ticketRemarkDraft"
          :status-options="statusOptions"
          :ticket-page-no="ticketPageNo"
          :ticket-page-size="ticketPageSize"
          :ticket-total="ticketTotal"
          @open-create-ticket="openCreateTicket"
          @update-status-draft="(ticketId, value) => ticketStatusDraft[ticketId] = value"
          @update-owner-draft="(ticketId, value) => ticketOwnerDraft[ticketId] = value"
          @update-remark-draft="(ticketId, value) => ticketRemarkDraft[ticketId] = value"
          @update-ticket-status="updateTicketStatus"
          @update-ticket-owner="updateTicketOwner"
          @update-ticket-remark="updateTicketRemark"
          @open-timeline="openTimeline"
          @draft-faq="draftFaq"
          @draft-sop="draftSop"
          @ticket-page-change="onTicketPageChange"
        />
      </article>
    </div>

    <n-modal v-model:show="createVisible" preset="card" title="创建工单" class="max-w-lg">
      <div class="space-y-3">
        <n-select v-model:value="createForm.priority" :options="priorityOptions" />
        <n-input v-model:value="createForm.content" type="textarea" :rows="4" placeholder="请输入工单内容" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button @click="createVisible = false">取消</n-button>
          <n-button type="primary" :loading="sending" @click="createTicket">提交工单</n-button>
        </div>
      </template>
    </n-modal>

    <n-modal v-model:show="timelineVisible" preset="card" title="工单处理历史" class="max-w-5xl">
      <div class="space-y-3">
        <div class="flex justify-end">
          <n-button class="nd-soft-focus" size="small" :disabled="!ticketTimeline?.auditQuery?.bizNo" @click="openAudit(ticketTimeline?.auditQuery)">
            查看审计记录
          </n-button>
        </div>
        <n-data-table :columns="timelineColumns" :data="ticketTimeline?.timeline || []" :bordered="false" size="small" />
      </div>
    </n-modal>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { NAlert, NButton, NDataTable, NInput, NModal, NSelect, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
import CustomerSessionList from "@/components/cs/CustomerSessionList.vue";
import CustomerSuggestionPanel from "@/components/cs/CustomerSuggestionPanel.vue";
import CustomerTicketList from "@/components/cs/CustomerTicketList.vue";
import { csApi, type CsAiSuggestion, type CsMessage, type CsSession, type CsTicket, type TicketTimelineItem, type TicketTimelineResp } from "@/services/customerService";
import { knowledgeApi } from "@/services/knowledge";

const router = useRouter();
const ui = useMessage();
const loadingSessions = ref(false);
const loadingSuggestion = ref(false);
const sending = ref(false);
const errorText = ref("");
const successText = ref("");
const autoReplyEnabled = ref(true);
const sessions = ref<CsSession[]>([]);
const activeSessionId = ref<number | null>(null);
const messages = ref<CsMessage[]>([]);
const aiSuggestion = ref<CsAiSuggestion | null>(null);
const tickets = ref<CsTicket[]>([]);
const ticketPageNo = ref(1);
const ticketPageSize = ref(5);
const ticketTotal = ref(0);
const inputText = ref("");
const createVisible = ref(false);
const timelineVisible = ref(false);
const ticketTimeline = ref<TicketTimelineResp | null>(null);

const createForm = reactive({ priority: "MEDIUM", content: "" });
const ticketStatusDraft = reactive<Record<string, string>>({});
const ticketOwnerDraft = reactive<Record<string, string>>({});
const ticketRemarkDraft = reactive<Record<string, string>>({});

const statusOptions = ["OPEN", "PROCESSING", "RESOLVED", "CLOSED"].map((v) => ({ label: v, value: v }));
const priorityOptions = [
  { label: "高优先级(HIGH)", value: "HIGH" },
  { label: "中优先级(MEDIUM)", value: "MEDIUM" },
  { label: "低优先级(LOW)", value: "LOW" }
];
const activeSession = computed(() => sessions.value.find((v) => v.id === activeSessionId.value) || null);
const suggestionFirst = computed(() => (aiSuggestion.value?.replyCandidates || [])[0] || "");

const timelineColumns: DataTableColumns<TicketTimelineItem> = [
  { title: "操作时间", key: "occurredAt", width: 180 },
  { title: "操作人", key: "operatorName", width: 120 },
  { title: "动作", key: "actionLabel", width: 150 },
  { title: "状态变化", key: "status", width: 180, render: (row) => `${row.statusFrom || "-"} -> ${row.statusTo || "-"}` },
  { title: "备注/意见", key: "note" }
];

async function loadSessions() {
  loadingSessions.value = true;
  errorText.value = "";
  try {
    sessions.value = await csApi.listSessions();
    if (!activeSessionId.value && sessions.value.length > 0) await selectSession(sessions.value[0].id);
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "会话加载失败";
  } finally {
    loadingSessions.value = false;
  }
}

async function selectSession(id: number) {
  activeSessionId.value = id;
  ticketPageNo.value = 1;
  await Promise.all([loadMessages(), loadSuggestion(), loadTickets()]);
}

async function loadMessages() {
  if (!activeSessionId.value) return;
  messages.value = await csApi.listMessages(activeSessionId.value);
}

async function loadSuggestion() {
  if (!activeSessionId.value) return;
  loadingSuggestion.value = true;
  try {
    aiSuggestion.value = await csApi.aiSuggestions(activeSessionId.value);
  } catch (e) {
    ui.error(e instanceof Error ? e.message : "AI建议加载失败");
  } finally {
    loadingSuggestion.value = false;
  }
}

async function sendAgentMessage() {
  if (!activeSessionId.value || !inputText.value.trim()) return;
  sending.value = true;
  try {
    await csApi.sendMessage(activeSessionId.value, { content: inputText.value.trim(), senderType: "AGENT", autoReply: false });
    inputText.value = "";
    successText.value = "人工消息已发送";
    await Promise.all([loadMessages(), loadSuggestion()]);
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "发送失败";
  } finally {
    sending.value = false;
  }
}

async function sendCustomerMessage() {
  if (!activeSessionId.value || !inputText.value.trim()) return;
  sending.value = true;
  try {
    await csApi.sendMessage(activeSessionId.value, { content: inputText.value.trim(), senderType: "CUSTOMER", autoReply: autoReplyEnabled.value });
    inputText.value = "";
    successText.value = autoReplyEnabled.value ? "客户提问已触发AI自动回复，可继续人工接管" : "客户提问已记录";
    await Promise.all([loadMessages(), loadSuggestion(), loadTickets(), loadSessions()]);
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "发送失败";
  } finally {
    sending.value = false;
  }
}

async function transferHuman() {
  if (!activeSessionId.value) return;
  try {
    await csApi.transferHuman(activeSessionId.value, 1);
    successText.value = "已标记人工接管";
    await Promise.all([loadSessions(), loadTickets()]);
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "接管失败";
  }
}

function openCreateTicket() {
  if (!activeSessionId.value) return;
  createForm.priority = aiSuggestion.value?.prioritySuggestion || "MEDIUM";
  createForm.content = aiSuggestion.value?.latestCustomerText || "";
  createVisible.value = true;
}

async function createTicket() {
  if (!activeSessionId.value) return;
  sending.value = true;
  try {
    await csApi.createTicket({ sessionId: activeSessionId.value, priority: createForm.priority, content: createForm.content.trim() });
    createVisible.value = false;
    successText.value = "工单已创建";
    await loadTickets();
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "创建工单失败";
  } finally {
    sending.value = false;
  }
}

async function loadTickets() {
  if (!activeSessionId.value) return;
  const resp = await csApi.listTickets(activeSessionId.value, ticketPageNo.value, ticketPageSize.value);
  tickets.value = resp.list || [];
  ticketTotal.value = Number(resp.total || 0);
  tickets.value.forEach((t) => {
    ticketStatusDraft[t.ticketId] = t.status;
    ticketOwnerDraft[t.ticketId] = String(t.assigneeUserId || "");
    ticketRemarkDraft[t.ticketId] = t.remark || "";
  });
}

async function onTicketPageChange(page: number) {
  ticketPageNo.value = page;
  await loadTickets();
}

async function updateTicketStatus(ticket: CsTicket) {
  try {
    await csApi.updateTicketStatus(
      ticket.ticketId,
      ticketStatusDraft[ticket.ticketId] || ticket.status,
      ticketRemarkDraft[ticket.ticketId] || ""
    );
    successText.value = "工单状态已更新";
    await loadTickets();
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "更新失败";
  }
}

async function updateTicketOwner(ticket: CsTicket) {
  const owner = Number(ticketOwnerDraft[ticket.ticketId] || "0");
  if (!owner) {
    ui.warning("请输入有效负责人ID");
    return;
  }
  try {
    await csApi.updateTicketOwner(ticket.ticketId, owner);
    successText.value = "负责人已更新";
    await loadTickets();
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "更新失败";
  }
}

async function updateTicketRemark(ticket: CsTicket) {
  try {
    await csApi.updateTicketRemark(ticket.ticketId, ticketRemarkDraft[ticket.ticketId] || "");
    successText.value = "处理备注已更新";
    await loadTickets();
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "更新失败";
  }
}

async function openTimeline(ticketId: string) {
  try {
    ticketTimeline.value = await csApi.ticketTimeline(ticketId);
    timelineVisible.value = true;
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "时间线加载失败";
  }
}

async function draftFaq(ticket: CsTicket) {
  try {
    await knowledgeApi.draftFaqFromTicket(ticket.ticketId);
    successText.value = "已从工单生成 FAQ 草稿，请到知识维护页确认启用";
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "FAQ 草稿生成失败";
  }
}

async function draftSop(ticket: CsTicket) {
  try {
    await knowledgeApi.draftSopFromTicket(ticket.ticketId);
    successText.value = "已从工单生成 SOP 草稿，请到知识维护页确认启用";
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "SOP 草稿生成失败";
  }
}

function openAudit(query?: { bizNo?: string; resourceType?: string; resourceId?: string }) {
  router.push({ path: "/system/audit-center", query: { bizNo: query?.bizNo, resourceType: query?.resourceType, resourceId: query?.resourceId } });
}

onMounted(loadSessions);
</script>
