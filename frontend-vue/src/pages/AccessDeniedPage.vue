<template>
  <section class="mx-auto mt-10 max-w-3xl space-y-5">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Access</p>
          <h1 class="nd-page-title">当前页面暂不可用</h1>
          <p class="nd-page-subtitle">你的角色是“{{ authStore.roleNameZh }}”，当前页面需要额外权限。</p>
        </div>
      </div>
    </header>

    <n-alert class="nd-state-alert" type="warning" :show-icon="false">
      权限点：{{ requiredPermission || "未知" }}。如需访问，请联系管理员授权，或先返回你的常用工作入口。
    </n-alert>

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <h3 class="nd-section-title">推荐下一步</h3>
      </div>
      <div class="nd-table-body grid grid-cols-1 gap-2 md:grid-cols-3">
        <n-button class="nd-soft-focus" type="primary" @click="goDashboard">返回角色首页</n-button>
        <n-button class="nd-soft-focus" @click="goRolePrimary">前往我的常用模块</n-button>
        <n-button class="nd-soft-focus" @click="goBack">返回上一页</n-button>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { NAlert, NButton } from "naive-ui";
import { useAuthStore } from "@/stores/auth";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const requiredPermission = computed(() => String(route.query.permission ?? ""));

async function goDashboard() {
  await router.replace("/dashboard");
}

async function goRolePrimary() {
  const role = authStore.roleKey;
  if (role === "warehouse_ops") {
    await router.replace("/wms/inbound");
    return;
  }
  if (role === "cs_ops") {
    await router.replace("/cs/workspace");
    return;
  }
  if (role === "observer") {
    await router.replace("/agent/center");
    return;
  }
  await router.replace("/dashboard");
}

async function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  await router.replace("/dashboard");
}
</script>
