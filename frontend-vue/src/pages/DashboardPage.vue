<template>
  <section class="space-y-4">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">仪表盘</h1>
        <p class="text-sm text-text-secondary">展示仓储与 AI 关键指标，支持本地 Docker 联调。</p>
      </div>
      <n-button :loading="loading" @click="loadData">刷新数据</n-button>
    </header>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
      <article v-for="card in cards" :key="card.title" class="rounded-2xl border border-border bg-surface p-4 shadow-card transition-all hover:-translate-y-1">
        <p class="text-sm text-text-secondary">{{ card.title }}</p>
        <p class="mt-2 text-3xl font-semibold tracking-tight">{{ card.value }}</p>
        <p class="mt-2 text-xs text-text-secondary">{{ card.description }}</p>
      </article>
    </div>

    <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
      <article class="rounded-2xl border border-border bg-surface p-4 shadow-card">
        <h3 class="text-sm font-semibold">入库 / 出库趋势</h3>
        <v-chart class="mt-3 h-72" :option="trendOption" autoresize />
      </article>

      <article class="rounded-2xl border border-border bg-surface p-4 shadow-card">
        <h3 class="text-sm font-semibold">库存风险分布</h3>
        <v-chart class="mt-3 h-72" :option="riskOption" autoresize />
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { NButton, useMessage } from "naive-ui";
import VChart from "vue-echarts";
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { BarChart, PieChart } from "echarts/charts";
import { GridComponent, LegendComponent, TooltipComponent } from "echarts/components";
import { reportsApi } from "@/services/reports";

use([CanvasRenderer, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent]);

const message = useMessage();
const loading = ref(false);
const metrics = ref({ totalSku: 0, todayInbound: 0, todayOutbound: 0, lowStockCount: 0 });

const cards = computed(() => [
  { title: "SKU 总量", value: metrics.value.totalSku, description: "商品主数据总数" },
  { title: "今日入库单", value: metrics.value.todayInbound, description: "当天新建入库单" },
  { title: "今日出库单", value: metrics.value.todayOutbound, description: "当天新建出库单" },
  { title: "低库存预警", value: metrics.value.lowStockCount, description: "可用库存 <= 10" }
]);

const trendOption = computed(() => ({
  tooltip: { trigger: "axis" },
  legend: { top: 0 },
  grid: { left: 30, right: 20, bottom: 20, top: 35, containLabel: true },
  xAxis: { type: "category", data: ["周一", "周二", "周三", "周四", "周五", "周六", "周日"] },
  yAxis: { type: "value" },
  series: [
    { name: "入库", type: "bar", data: [8, 12, 10, 15, 11, 9, metrics.value.todayInbound] },
    { name: "出库", type: "bar", data: [6, 10, 8, 13, 9, 7, metrics.value.todayOutbound] }
  ]
}));

const riskOption = computed(() => ({
  tooltip: { trigger: "item" },
  legend: { bottom: 0 },
  series: [
    {
      type: "pie",
      radius: ["42%", "70%"],
      data: [
        { value: Math.max(metrics.value.lowStockCount, 1), name: "低库存" },
        { value: Math.max(metrics.value.totalSku - metrics.value.lowStockCount, 1), name: "健康库存" }
      ]
    }
  ]
}));

async function loadData() {
  loading.value = true;
  try {
    metrics.value = await reportsApi.dashboard();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "仪表盘加载失败");
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>
