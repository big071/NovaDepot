<template>
  <section class="grid grid-cols-1 gap-4 xl:grid-cols-[300px_1fr_320px]">
    <article class="rounded-2xl border border-border bg-surface p-4 shadow-card">
      <div class="mb-3 flex items-center justify-between">
        <h2 class="text-base font-semibold">会话列表</h2>
        <n-button size="small" :loading="loadingSessions" @click="loadSessions">刷新</n-button>
      </div>
      <div class="space-y-2">
        <button
          v-for="session in sessions"
          :key="session.id"
          class="w-full rounded-xl border px-3 py-2 text-left text-sm transition-all"
          :class="activeSessionId === session.id ? 'border-primary bg-primary/10' : 'border-border hover:border-primary/40 hover:bg-bg'"
          @click="selectSession(session.id)"
        >
          <p class="font-medium">{{ session.sessionNo }}</p>
          <p class="mt-1 text-xs text-text-secondary">{{ session.status }} / {{ session.priority }}</p>
        </button>
      </div>
    </article>

    <article class="rounded-2xl border border-border bg-surface p-4 shadow-card">
      <header class="mb-4">
        <h1 class="text-xl font-semibold tracking-tight">智能客服工作台</h1>
        <p class="text-sm text-text-secondary">支持会话消息、转人工、工单创建。</p>
      </header>

      <div class="h-[420px] space-y-3 overflow-y-auto rounded-xl border border-border bg-bg/70 p-3">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="max-w-[88%] rounded-xl px-3 py-2 text-sm"
          :class="msg.sender === 'CUSTOMER' ? 'mr-auto bg-surface shadow-sm' : 'ml-auto bg-primary text-white'"
        >
          <p class="text-xs opacity-70">{{ msg.sender }}</p>
          <p class="mt-1">{{ msg.content }}</p>
        </div>
      </div>

      <div class="mt-3 space-y-2">
        <n-alert v-if="errorText" type="error" :show-icon="false">{{ errorText }}</n-alert>
        <div class="flex gap-2">
          <n-input v-model:value="inputText" placeholder="输入回复内容" @keyup.enter="onSend(false)" />
          <n-button :loading="sending" @click="onSend(false)">人工发送</n-button>
          <n-button type="primary" :loading="sending" @click="onSend(true)">AI 发送</n-button>
        </div>
      </div>
    </article>

    <article class="space-y-4">
      <div class="rounded-2xl border border-border bg-surface p-4 shadow-card">
        <h3 class="text-sm font-semibold">快捷操作</h3>
        <div class="mt-3 space-y-2">
          <n-button block secondary :loading="actionLoading" @click="transferHuman">转人工（用户ID=1）</n-button>
          <n-button block secondary :loading="actionLoading" @click="createTicket">创建工单</n-button>
        </div>
      </div>

      <div class="rounded-2xl border border-border bg-surface p-4 shadow-card">
        <div class="mb-3 flex items-center justify-between gap-2">
          <h3 class="text-sm font-semibold">FAQ</h3>
          <n-button size="small" :loading="loadingFaq" @click="loadFaq">查询</n-button>
        </div>
        <n-input v-model:value="faqKeyword" size="small" placeholder="按关键词过滤 FAQ" />
        <div class="mt-3 space-y-2">
          <article v-for="item in faqList" :key="item.id" class="rounded-xl border border-border bg-bg/60 p-3">
            <p class="text-sm font-medium">{{ item.question }}</p>
            <p class="mt-1 text-xs text-text-secondary">{{ item.answer }}</p>
          </article>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { NAlert, NButton, NInput, useMessage } from "naive-ui";
import { csApi, type CsMessage, type CsSession, type FaqItem } from "@/services/customerService";

const uiMessage = useMessage();
const loadingSessions = ref(false);
const loadingFaq = ref(false);
const sending = ref(false);
const actionLoading = ref(false);
const errorText = ref("");
const inputText = ref("");
const faqKeyword = ref("");

const sessions = ref<CsSession[]>([]);
const activeSessionId = ref<number | null>(null);
const messages = ref<CsMessage[]>([]);
const faqList = ref<FaqItem[]>([]);

async function loadSessions() {
  loadingSessions.value = true;
  try {
    sessions.value = await csApi.listSessions();
    if (!activeSessionId.value && sessions.value.length > 0) {
      await selectSession(sessions.value[0].id);
    }
  } catch (error) {
    uiMessage.error(error instanceof Error ? error.message : "会话列表加载失败");
  } finally {
    loadingSessions.value = false;
  }
}

async function selectSession(sessionId: number) {
  activeSessionId.value = sessionId;
  await loadMessages(sessionId);
}

async function loadMessages(sessionId: number) {
  try {
    messages.value = await csApi.listMessages(sessionId);
  } catch (error) {
    uiMessage.error(error instanceof Error ? error.message : "消息加载失败");
  }
}

async function onSend(sendByAi: boolean) {
  if (!activeSessionId.value) {
    errorText.value = "请先选择会话";
    uiMessage.warning("请先选择会话");
    return;
  }
  if (!inputText.value.trim()) {
    uiMessage.warning("请输入消息内容后再发送");
    return;
  }

  errorText.value = "";
  sending.value = true;
  try {
    const resp = await csApi.sendMessage(activeSessionId.value, {
      content: inputText.value.trim(),
      msgType: "TEXT",
      sendByAi
    });
    messages.value.push(resp);
    inputText.value = "";
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "发送失败";
  } finally {
    sending.value = false;
  }
}

async function transferHuman() {
  if (!activeSessionId.value) {
    errorText.value = "请先选择会话";
    uiMessage.warning("请先选择会话");
    return;
  }
  actionLoading.value = true;
  try {
    await csApi.transferHuman(activeSessionId.value, 1);
    uiMessage.success("已转人工");
    await loadSessions();
  } catch (error) {
    uiMessage.error(error instanceof Error ? error.message : "转人工失败");
  } finally {
    actionLoading.value = false;
  }
}

async function createTicket() {
  if (!activeSessionId.value) {
    errorText.value = "请先选择会话";
    uiMessage.warning("请先选择会话");
    return;
  }
  actionLoading.value = true;
  try {
    const resp = await csApi.createTicket({
      sessionId: activeSessionId.value,
      priority: "MEDIUM",
      content: inputText.value.trim() || "来自客服工作台的工单"
    });
    uiMessage.success(`工单已创建: ${resp.ticketNo}`);
  } catch (error) {
    uiMessage.error(error instanceof Error ? error.message : "创建工单失败");
  } finally {
    actionLoading.value = false;
  }
}

async function loadFaq() {
  loadingFaq.value = true;
  try {
    faqList.value = await csApi.faq(faqKeyword.value || undefined);
  } catch (error) {
    uiMessage.error(error instanceof Error ? error.message : "FAQ 查询失败");
  } finally {
    loadingFaq.value = false;
  }
}

onMounted(async () => {
  await loadSessions();
  await loadFaq();
});
</script>
