<template>
  <section class="grid grid-cols-1 gap-4 xl:grid-cols-[320px_1fr]">
    <article class="rounded-2xl border border-border bg-surface p-4 shadow-card">
      <div class="mb-3 flex items-center justify-between">
        <h2 class="text-base font-semibold">AI 会话</h2>
        <n-button size="small" :loading="loadingConversations" @click="loadConversations">刷新</n-button>
      </div>
      <div class="space-y-2">
        <button
          v-for="item in conversations"
          :key="item.id"
          class="w-full rounded-xl border px-3 py-2 text-left text-sm transition-all"
          :class="activeConversationId === item.id ? 'border-primary bg-primary/10' : 'border-border hover:border-primary/40 hover:bg-bg'"
          @click="activeConversationId = item.id"
        >
          <p class="font-medium">{{ item.conversationNo }}</p>
          <p class="mt-1 text-xs text-text-secondary">{{ item.provider }} / {{ item.scene }}</p>
        </button>
      </div>
    </article>

    <article class="rounded-2xl border border-border bg-surface p-4 shadow-card">
      <header class="mb-4 flex items-center justify-between gap-2">
        <div>
          <h1 class="text-xl font-semibold tracking-tight">AI 助手</h1>
          <p class="text-sm text-text-secondary">免费方案优先：RuleProvider / MockProvider。</p>
        </div>
        <n-select v-model:value="scene" :options="sceneOptions" size="small" class="w-36" />
      </header>

      <div class="h-[420px] space-y-3 overflow-y-auto rounded-xl border border-border bg-bg/70 p-3">
        <div
          v-for="(item, index) in activeMessages"
          :key="index"
          class="max-w-[85%] rounded-xl px-3 py-2 text-sm"
          :class="item.role === 'user' ? 'ml-auto bg-primary text-white' : 'bg-surface text-text-primary shadow-sm'"
        >
          <p>{{ item.content }}</p>
        </div>
      </div>

      <div class="mt-3 space-y-2">
        <n-alert v-if="errorText" type="error" :show-icon="false">{{ errorText }}</n-alert>
        <div class="flex gap-2">
          <n-input v-model:value="inputText" placeholder="输入问题并发送" @keyup.enter="sendMessage" />
          <n-button type="primary" :loading="sending" @click="sendMessage">发送</n-button>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { NAlert, NButton, NInput, NSelect, useMessage } from "naive-ui";
import { aiApi, type AiConversation } from "@/services/ai";

type ChatMessage = { role: "user" | "assistant"; content: string };

const uiMessage = useMessage();
const loadingConversations = ref(false);
const sending = ref(false);
const errorText = ref("");
const scene = ref("enterprise");
const inputText = ref("");
const conversations = ref<AiConversation[]>([]);
const activeConversationId = ref<number | null>(null);
const messageMap = ref<Record<number, ChatMessage[]>>({});

const sceneOptions = [
  { label: "企业助手", value: "enterprise" },
  { label: "仓储助手", value: "warehouse" },
  { label: "SOP 助手", value: "sop" }
];

const activeMessages = computed(() => {
  if (!activeConversationId.value) return [];
  return messageMap.value[activeConversationId.value] ?? [];
});

async function loadConversations() {
  loadingConversations.value = true;
  try {
    conversations.value = await aiApi.conversations();
    if (!activeConversationId.value && conversations.value.length > 0) {
      activeConversationId.value = conversations.value[0].id;
    }
  } catch (error) {
    uiMessage.error(error instanceof Error ? error.message : "会话列表加载失败");
  } finally {
    loadingConversations.value = false;
  }
}

async function sendMessage() {
  if (!inputText.value.trim()) {
    uiMessage.warning("请输入问题后再发送");
    return;
  }

  errorText.value = "";
  const content = inputText.value.trim();
  inputText.value = "";

  const tempConversationId = activeConversationId.value ?? -1;
  if (!messageMap.value[tempConversationId]) {
    messageMap.value[tempConversationId] = [];
  }
  messageMap.value[tempConversationId].push({ role: "user", content });

  sending.value = true;
  try {
    const resp = await aiApi.chat({
      scene: scene.value,
      message: content,
      conversationId: activeConversationId.value ?? undefined,
      providerHint: "rule"
    });

    if (tempConversationId !== resp.conversationId && messageMap.value[tempConversationId]) {
      messageMap.value[resp.conversationId] = [...messageMap.value[tempConversationId]];
      delete messageMap.value[tempConversationId];
    }

    activeConversationId.value = resp.conversationId;
    if (!messageMap.value[resp.conversationId]) {
      messageMap.value[resp.conversationId] = [];
    }
    messageMap.value[resp.conversationId].push({ role: "assistant", content: resp.reply });

    await loadConversations();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "AI 回复失败";
  } finally {
    sending.value = false;
  }
}

onMounted(loadConversations);
</script>
