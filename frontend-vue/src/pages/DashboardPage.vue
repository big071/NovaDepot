<template>
  <section class="space-y-4">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Workbench</p>
          <h1 class="nd-page-title">{{ title }}</h1>
          <p class="nd-page-subtitle">{{ subtitle }}</p>
        </div>
        <n-button class="nd-soft-focus" :loading="loading" @click="loadData">刷新</n-button>
      </div>
    </header>

    <n-alert v-if="errorText" type="error" :show-icon="false">{{ errorText }}</n-alert>

    <div class="grid gap-4 md:grid-cols-4">
      <article class="rounded-xl border border-border bg-bg/50 p-3">
        <p class="text-xs text-text-secondary">SKU总数</p>
        <p class="mt-1 text-2xl font-semibold">{{ data.metrics.totalSku }}</p>
      </article>
      <article class="rounded-xl border border-border bg-bg/50 p-3">
        <p class="text-xs text-text-secondary">今日入库单</p>
        <p class="mt-1 text-2xl font-semibold">{{ data.metrics.todayInbound }}</p>
      </article>
      <article class="rounded-xl border border-border bg-bg/50 p-3">
        <p class="text-xs text-text-secondary">今日出库单</p>
        <p class="mt-1 text-2xl font-semibold">{{ data.metrics.todayOutbound }}</p>
      </article>
      <article class="rounded-xl border border-border bg-bg/50 p-3">
        <p class="text-xs text-text-secondary">低库存提醒</p>
        <p class="mt-1 text-2xl font-semibold">{{ data.metrics.lowStockCount }}</p>
      </article>
    </div>

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <h3 class="nd-section-title">角色化待办区</h3>
      </div>
      <div class="nd-table-body">
        <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <article v-for="item in todoCards" :key="item.key" class="rounded-xl border border-border bg-bg/60 p-3">
            <p class="text-sm text-text-secondary">{{ item.label }}</p>
            <p class="mt-1 text-2xl font-semibold">{{ item.value }}</p>
          </article>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { NAlert, NButton } from "naive-ui";
import { reportsApi, type DashboardTodoResp } from "@/services/reports";
import { useAuthStore } from "@/stores/auth";

const authStore = useAuthStore();
const loading = ref(false);
const errorText = ref("");
const data = reactive<DashboardTodoResp>({
  roleKey: "observer",
  metrics: { totalSku: 0, todayInbound: 0, todayOutbound: 0, lowStockCount: 0 },
  todos: {}
});

const title = computed(() => {
  if (authStore.roleKey === "admin") return "管理员首页";
  if (authStore.roleKey === "warehouse_ops") return "仓储运营首页";
  if (authStore.roleKey === "cs_ops") return "客服运营首页";
  return "观察员首页";
});

const subtitle = computed(() => {
  if (authStore.roleKey === "admin") return "重点关注待审核单据、异常和审计风险。";
  if (authStore.roleKey === "warehouse_ops") return "重点关注待提交、被驳回和待执行单据。";
  if (authStore.roleKey === "cs_ops") return "重点关注待回复会话、待处理工单和AI建议。";
  return "只读查看今日业务概览和风险摘要。";
});

const todoCards = computed(() => {
  const t = data.todos as Record<string, number> & {
    riskSummary?: Record<string, number>;
    todayOverview?: Record<string, number>;
  };
  if (data.roleKey === "admin") {
    return [
      { key: "pendingInboundApproval", label: "待审核入库单", value: t.pendingInboundApproval ?? 0 },
      { key: "pendingOutboundApproval", label: "待审核出库单", value: t.pendingOutboundApproval ?? 0 },
      { key: "pendingExceptions", label: "待处理异常", value: t.pendingExceptions ?? 0 },
      { key: "recentAuditFailures", label: "最近审计异常", value: t.recentAuditFailures ?? 0 }
    ];
  }
  if (data.roleKey === "warehouse_ops") {
    return [
      { key: "pendingDrafts", label: "待提交草稿", value: t.pendingDrafts ?? 0 },
      { key: "rejectedDocuments", label: "被驳回单据", value: t.rejectedDocuments ?? 0 },
      { key: "pendingExecution", label: "待执行入库/出库", value: t.pendingExecution ?? 0 },
      { key: "lowStockCount", label: "低库存提醒", value: t.lowStockCount ?? 0 }
    ];
  }
  if (data.roleKey === "cs_ops") {
    return [
      { key: "pendingReplies", label: "待回复会话", value: t.pendingReplies ?? 0 },
      { key: "pendingTickets", label: "待处理工单", value: t.pendingTickets ?? 0 },
      { key: "aiPendingConfirm", label: "AI建议待确认", value: t.aiPendingConfirm ?? 0 }
    ];
  }
  return [
    { key: "risk_lowStock", label: "只读风险摘要-低库存", value: t.riskSummary?.lowStockCount ?? 0 },
    { key: "risk_pendingApproval", label: "只读风险摘要-待审核", value: t.riskSummary?.pendingApproval ?? 0 },
    { key: "todayInbound", label: "今日业务概览-入库", value: t.todayOverview?.todayInbound ?? 0 },
    { key: "todayOutbound", label: "今日业务概览-出库", value: t.todayOverview?.todayOutbound ?? 0 }
  ];
});

async function loadData() {
  loading.value = true;
  errorText.value = "";
  try {
    const resp = await reportsApi.dashboardTodos();
    data.roleKey = resp.roleKey;
    data.metrics = resp.metrics;
    data.todos = resp.todos;
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : "首页数据加载失败";
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>
