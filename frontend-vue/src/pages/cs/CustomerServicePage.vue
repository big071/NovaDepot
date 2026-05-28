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
      <article class="nd-table-shell">
        <div class="nd-table-head"><h3 class="nd-section-title">会话列表</h3></div>
        <div class="nd-table-body space-y-2">
          <n-empty v-if="!loadingSessions && sessions.length === 0" class="nd-empty-shell" description="暂无会话" />
          <button v-for="item in sessions" :key="item.id" class="w-full rounded-xl border p-3 text-left transition"
            :class="activeSessionId === item.id ? 'border-primary bg-primary/10' : 'border-border bg-bg/50 hover:border-primary/40'"
            @click="selectSession(item.id)">
            <div class="flex items-center justify-between">
              <p class="text-sm font-medium">{{ item.sessionNo }}</p>
              <n-tag :bordered="false" size="small" :type="item.priority === 'HIGH' ? 'error' : 'warning'">{{ item.priority }}</n-tag>
            </div>
            <p class="mt-1 text-xs text-text-secondary">状态：{{ item.status }}</p>
            <p class="mt-1 text-xs text-text-secondary">处理模式：{{ item.handoffStatus === 'HUMAN_ASSIGNED' ? '人工接管中' : 'AI优先' }}</p>
          </button>
        </div>
      </article>

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

        <article class="nd-table-shell">
          <div class="nd-table-head">
            <h3 class="nd-section-title">消息与AI建议</h3>
          </div>
          <div class="nd-table-body space-y-3">
            <div class="flex flex-wrap gap-2">
              <n-button class="nd-soft-focus" :disabled="!activeSessionId" :loading="loadingSuggestion" @click="loadSuggestion">获取AI建议</n-button>
              <n-button class="nd-soft-focus" :disabled="!activeSessionId || !suggestionFirst" @click="inputText = suggestionFirst">套用首条建议</n-button>
              <n-button class="nd-soft-focus" :disabled="!activeSessionId" @click="transferHuman">人工接管</n-button>
              <n-button class="nd-soft-focus" :disabled="!activeSessionId" @click="autoReplyEnabled = !autoReplyEnabled">
                自动回复：{{ autoReplyEnabled ? "开启" : "关闭" }}
              </n-button>
            </div>
            <article class="rounded-xl border border-border bg-bg/60 p-3 text-sm">
              <p class="font-medium">AI建议依据</p>
              <p class="mt-1 text-text-secondary">{{ (aiSuggestion?.basis || []).join("；") || "-" }}</p>
              <p class="mt-2 font-medium">工单分类建议：{{ aiSuggestion?.ticketCategorySuggestion || "-" }}</p>
              <p class="mt-1">优先级建议：{{ aiSuggestion?.prioritySuggestion || "-" }}</p>
              <p class="mt-1">SOP建议：{{ aiSuggestion?.sopSuggestion || "-" }}</p>
              <p class="mt-2 font-medium">知识引用来源</p>
              <div v-if="(aiSuggestion?.knowledgeRefs || []).length" class="mt-2 flex flex-wrap gap-2">
                <n-tag v-for="ref in aiSuggestion?.knowledgeRefs || []" :key="`${ref.type}-${ref.code || ref.title}`" :bordered="false" type="info">
                  {{ ref.type }}：{{ ref.title }} / {{ ref.scene || '通用' }}
                </n-tag>
              </div>
              <p v-else class="mt-1 text-text-secondary">{{ aiSuggestion?.knowledgeFallbackNotice || "未命中知识库，当前建议来自规则回退。" }}</p>
              <p class="mt-2 text-xs text-text-secondary" v-if="aiSuggestion?.ruleConfigBasis">
                规则配置：自动回复 {{ aiSuggestion.ruleConfigBasis.autoReplyPriority }}；候选回复 {{ aiSuggestion.ruleConfigBasis.candidateReplyPriority }}
              </p>
            </article>
            <div class="max-h-[260px] space-y-2 overflow-y-auto rounded-xl border border-border bg-bg/40 p-3">
              <article v-for="msg in messages" :key="msg.id" class="rounded-lg border border-border bg-surface p-2 text-sm">
                <p class="text-xs text-text-secondary">{{ msg.sender }} · {{ msg.msgType }}</p>
                <p class="mt-1">{{ msg.content }}</p>
              </article>
            </div>
            <div class="flex gap-2">
              <n-input v-model:value="inputText" placeholder="输入消息内容，回车发送" @keyup.enter="sendAgentMessage" />
              <n-button :loading="sending" :disabled="!activeSessionId || !inputText.trim()" @click="sendAgentMessage">人工发送</n-button>
              <n-button type="primary" :loading="sending" :disabled="!activeSessionId || !inputText.trim()" @click="sendCustomerMessage">模拟客户提问</n-button>
            </div>
          </div>
        </article>

        <article class="nd-table-shell">
          <div class="nd-table-head">
            <h3 class="nd-section-title">工单列表</h3>
            <n-button class="nd-soft-focus" :disabled="!activeSessionId" @click="openCreateTicket">创建工单</n-button>
          </div>
          <div class="nd-table-body space-y-3">
            <article v-for="ticket in tickets" :key="ticket.ticketId" class="rounded-xl border border-border bg-bg/60 p-3 text-sm">
              <div class="flex items-center justify-between">
                <p class="font-medium">{{ ticket.ticketNo }}</p>
                <n-tag :bordered="false" type="info">{{ ticket.status }}</n-tag>
              </div>
              <p class="mt-1 text-text-secondary">责任人：{{ ticket.assigneeUserId || "-" }}</p>
              <p class="mt-1 text-text-secondary">AI自动回复：{{ ticket.aiAutoReplied ? "是" : "否" }} / 人工接管：{{ ticket.humanTakenOver ? "是" : "否" }}</p>
              <p class="mt-1 text-text-secondary">下一步建议：{{ ticket.nextSuggestion || "-" }}</p>
              <div class="mt-2 grid gap-2 md:grid-cols-3">
                <n-select v-model:value="ticketStatusDraft[ticket.ticketId]" :options="statusOptions" />
                <n-input v-model:value="ticketOwnerDraft[ticket.ticketId]" placeholder="负责人ID" />
                <n-input v-model:value="ticketRemarkDraft[ticket.ticketId]" placeholder="处理备注/关闭原因" />
              </div>
              <div class="mt-2 flex flex-wrap gap-2">
                <n-button size="small" @click="updateTicketStatus(ticket)">保存状态</n-button>
                <n-button size="small" @click="updateTicketOwner(ticket)">保存负责人</n-button>
                <n-button size="small" @click="updateTicketRemark(ticket)">保存备注</n-button>
                <n-button size="small" type="primary" @click="openTimeline(ticket.ticketId)">查看处理历史</n-button>
                <n-button size="small" @click="draftFaq(ticket)">沉淀FAQ草稿</n-button>
                <n-button size="small" @click="draftSop(ticket)">沉淀SOP草稿</n-button>
              </div>
            </article>
            <n-pagination :page="ticketPageNo" :page-size="ticketPageSize" :item-count="ticketTotal" @update:page="onTicketPageChange" />
          </div>
        </article>
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
import { NAlert, NButton, NDataTable, NInput, NModal, NPagination, NSelect, NTag, useMessage } from "naive-ui";
import type { DataTableColumns } from "naive-ui";
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
