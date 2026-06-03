<template>
  <article class="nd-table-shell">
    <div class="nd-table-head">
      <h3 class="nd-section-title">工单列表</h3>
      <n-button class="nd-soft-focus" aria-label="创建工单" :disabled="!activeSessionId" @click="$emit('open-create-ticket')">创建工单</n-button>
    </div>
    <div class="nd-table-body space-y-3">
      <article v-for="ticket in tickets" :key="ticket.ticketId" class="rounded-xl border border-border bg-bg/60 p-3 text-sm">
        <div class="flex items-center justify-between">
          <p class="font-medium">{{ ticket.ticketNo }}</p>
          <n-tag :bordered="false" type="info">{{ ticket.status }}</n-tag>
        </div>
        <p class="mt-1 text-text-secondary">负责人：{{ ticket.assigneeUserId || "-" }}</p>
        <p class="mt-1 text-text-secondary">AI 自动回复：{{ ticket.aiAutoReplied ? "是" : "否" }} / 人工接管：{{ ticket.humanTakenOver ? "是" : "否" }}</p>
        <p class="mt-1 text-text-secondary">下一步建议：{{ ticket.nextSuggestion || "-" }}</p>
        <div class="mt-2 grid gap-2 md:grid-cols-3">
          <n-select :value="ticketStatusDraft[ticket.ticketId]" :options="statusOptions" @update:value="(value) => $emit('update-status-draft', ticket.ticketId, value)" />
          <n-input :value="ticketOwnerDraft[ticket.ticketId]" placeholder="负责人 ID" @update:value="(value) => $emit('update-owner-draft', ticket.ticketId, value)" />
          <n-input :value="ticketRemarkDraft[ticket.ticketId]" placeholder="处理备注/关闭原因" @update:value="(value) => $emit('update-remark-draft', ticket.ticketId, value)" />
        </div>
        <div class="mt-2 flex flex-wrap gap-2">
          <n-button size="small" @click="$emit('update-ticket-status', ticket)">保存状态</n-button>
          <n-button size="small" @click="$emit('update-ticket-owner', ticket)">保存负责人</n-button>
          <n-button size="small" @click="$emit('update-ticket-remark', ticket)">保存备注</n-button>
          <n-button size="small" type="primary" @click="$emit('open-timeline', ticket.ticketId)">查看处理历史</n-button>
          <n-button size="small" @click="$emit('draft-faq', ticket)">沉淀 FAQ 草稿</n-button>
          <n-button size="small" @click="$emit('draft-sop', ticket)">沉淀 SOP 草稿</n-button>
        </div>
      </article>
      <n-pagination :page="ticketPageNo" :page-size="ticketPageSize" :item-count="ticketTotal" @update:page="(page) => $emit('ticket-page-change', page)" />
    </div>
  </article>
</template>

<script setup lang="ts">
import { NButton, NInput, NPagination, NSelect, NTag } from "naive-ui";
import type { CsTicket } from "@/services/customerService";

defineProps<{
  activeSessionId: number | null;
  tickets: CsTicket[];
  ticketStatusDraft: Record<string, string>;
  ticketOwnerDraft: Record<string, string>;
  ticketRemarkDraft: Record<string, string>;
  statusOptions: Array<{ label: string; value: string }>;
  ticketPageNo: number;
  ticketPageSize: number;
  ticketTotal: number;
}>();

defineEmits<{
  "open-create-ticket": [];
  "update-status-draft": [ticketId: string, value: string];
  "update-owner-draft": [ticketId: string, value: string];
  "update-remark-draft": [ticketId: string, value: string];
  "update-ticket-status": [ticket: CsTicket];
  "update-ticket-owner": [ticket: CsTicket];
  "update-ticket-remark": [ticket: CsTicket];
  "open-timeline": [ticketId: string];
  "draft-faq": [ticket: CsTicket];
  "draft-sop": [ticket: CsTicket];
  "ticket-page-change": [page: number];
}>();
</script>
