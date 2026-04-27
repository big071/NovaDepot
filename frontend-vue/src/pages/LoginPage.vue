<template>
  <div class="flex min-h-screen items-center justify-center px-4 py-8">
    <section class="w-full max-w-5xl rounded-3xl border border-border bg-surface/90 p-4 shadow-card backdrop-blur">
      <div class="grid grid-cols-1 gap-4 lg:grid-cols-[1.1fr_1fr]">
        <article class="nd-hero">
          <div class="nd-hero-header">
            <div>
              <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">NovaDepot</p>
              <h1 class="nd-page-title">智能仓储运营工作台</h1>
              <p class="nd-page-subtitle">登录后系统会根据你的角色进入对应工作台，不再提供演示账号一键填充。</p>
            </div>
          </div>
          <div class="nd-hero-meta">
            <span class="nd-pill">本地 Docker 环境</span>
            <span class="nd-pill">岗位化工作台</span>
          </div>
          <div class="mt-4 grid grid-cols-2 gap-3">
            <article class="nd-metric-chip">
              <p class="nd-metric-label">业务角色</p>
              <p class="nd-metric-value">4 类</p>
            </article>
            <article class="nd-metric-chip">
              <p class="nd-metric-label">核心模块</p>
              <p class="nd-metric-value">12 项</p>
            </article>
          </div>
          <article class="mt-4 rounded-xl border border-border bg-bg/50 p-3">
            <p class="text-sm font-semibold">测试角色账号</p>
            <p class="mt-1 text-xs text-text-secondary">请手动输入账号密码登录，验证不同角色功能。</p>
            <div class="mt-2 space-y-1 text-xs text-text-secondary">
              <p>管理员：`admin`</p>
              <p>仓储运营：`warehouse_manager`</p>
              <p>客服运营：`cs_agent`</p>
              <p>观察员：`viewer`</p>
            </div>
          </article>
        </article>

        <article class="rounded-3xl border border-border bg-surface p-6 shadow-sm">
          <p class="text-sm text-text-secondary">账户登录</p>
          <h2 class="mt-1 text-2xl font-semibold tracking-tight">欢迎回来</h2>

          <n-alert v-if="sessionExpiredText" class="nd-state-alert mt-4" type="warning" :show-icon="false">
            {{ sessionExpiredText }}
          </n-alert>
          <n-alert v-if="errorText" class="nd-state-alert mt-3" type="error" :show-icon="false">{{ errorText }}</n-alert>

          <div class="mt-5 space-y-3">
            <n-input v-model:value="form.tenantCode" class="nd-soft-focus" placeholder="租户编码" />
            <n-input v-model:value="form.username" class="nd-soft-focus" placeholder="请输入账号" />
            <n-input
              v-model:value="form.password"
              class="nd-soft-focus"
              type="password"
              show-password-on="click"
              placeholder="请输入密码"
              @keyup.enter="onLogin"
            />
            <p class="text-xs text-text-secondary">当前版本不开放注册，请联系管理员分配账号。</p>
            <n-button class="nd-soft-focus" type="primary" block :loading="authStore.loading" :disabled="!canSubmit" @click="onLogin">
              登录
            </n-button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { NAlert, NButton, NInput } from "naive-ui";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const errorText = ref("");
const sessionExpiredText = computed(() =>
  route.query.reason === "expired" ? "登录状态已失效，请重新登录后继续操作。" : ""
);

const form = reactive({
  tenantCode: "default",
  username: "",
  password: ""
});

const canSubmit = computed(() => {
  if (authStore.loading) return false;
  return Boolean(form.tenantCode.trim() && form.username.trim() && form.password.trim());
});

async function onLogin() {
  errorText.value = "";
  if (!form.tenantCode.trim() || !form.username.trim() || !form.password.trim()) return;
  try {
    await authStore.login(form);
    if (!authStore.profile) {
      throw new Error("未获取到角色信息，请重试登录。");
    }
    await router.push(defaultEntryPath());
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "登录失败";
  }
}

function defaultEntryPath() {
  const role = authStore.roleKey;
  if (role === "warehouse_ops") {
    if (authStore.hasPermission("INBOUND_READ")) return "/wms/inbound";
    if (authStore.hasPermission("OUTBOUND_READ")) return "/wms/outbound";
  }
  if (role === "cs_ops") {
    if (authStore.hasPermission("CS_SESSION_READ")) return "/cs/workspace";
    if (authStore.hasPermission("AI_CHAT")) return "/ai/enterprise";
  }
  if (role === "observer") {
    if (authStore.hasPermission("REPORT_DASHBOARD_READ")) return "/dashboard";
    if (authStore.hasPermission("AGENT_TASK_READ")) return "/agent/center";
  }
  if (authStore.hasPermission("REPORT_DASHBOARD_READ")) return "/dashboard";
  if (authStore.hasPermission("AI_CHAT")) return "/ai/enterprise";
  if (authStore.hasPermission("AGENT_TASK_READ")) return "/agent/center";
  return "/access-denied";
}

onMounted(() => {
  authStore.logout();
});
</script>
