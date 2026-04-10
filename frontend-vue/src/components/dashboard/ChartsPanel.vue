<template>
  <section class="grid grid-cols-1 gap-4 xl:grid-cols-2">
    <article class="glass-card rounded-2xl border border-border p-4 shadow-card">
      <h3 class="mb-3 text-sm font-semibold">出入库趋势</h3>
      <v-chart class="h-72" :option="trendOption" autoresize />
    </article>
    <article class="glass-card rounded-2xl border border-border p-4 shadow-card">
      <h3 class="mb-3 text-sm font-semibold">仓库库存占比</h3>
      <v-chart class="h-72" :option="pieOption" autoresize />
    </article>
  </section>
</template>

<script setup lang="ts">
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { BarChart, LineChart, PieChart } from "echarts/charts";
import { GridComponent, TooltipComponent, LegendComponent } from "echarts/components";
import VChart from "vue-echarts";

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent]);

const trendOption = {
  tooltip: { trigger: "axis" },
  legend: { textStyle: { color: "var(--text-secondary)" } },
  grid: { left: 36, right: 18, top: 32, bottom: 26 },
  xAxis: {
    type: "category",
    data: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
    axisLabel: { color: "var(--text-secondary)" }
  },
  yAxis: {
    type: "value",
    axisLabel: { color: "var(--text-secondary)" }
  },
  series: [
    {
      name: "入库",
      type: "bar",
      data: [120, 90, 140, 170, 160, 132, 156],
      itemStyle: { color: "#2563eb", borderRadius: [6, 6, 0, 0] }
    },
    {
      name: "出库",
      type: "line",
      smooth: true,
      data: [80, 110, 100, 130, 128, 118, 136],
      lineStyle: { width: 3, color: "#16a34a" },
      itemStyle: { color: "#16a34a" }
    }
  ]
};

const pieOption = {
  tooltip: { trigger: "item" },
  legend: { bottom: 4, textStyle: { color: "var(--text-secondary)" } },
  series: [
    {
      type: "pie",
      radius: ["40%", "72%"],
      data: [
        { value: 42, name: "上海一仓" },
        { value: 33, name: "深圳二仓" },
        { value: 25, name: "杭州中转仓" }
      ],
      label: { color: "var(--text-primary)" }
    }
  ]
};
</script>
