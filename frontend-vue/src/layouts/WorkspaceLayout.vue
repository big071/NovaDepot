<template>
  <div class="min-h-screen bg-bg text-text-primary">
    <div class="flex min-h-screen">
      <SidebarNav />
      <section class="flex min-h-screen flex-1 flex-col">
        <TopBar />
        <main class="flex-1 p-4 lg:p-6">
          <n-alert v-if="unauthorizedText" class="nd-state-alert mb-4" type="warning" :show-icon="false">
            <div class="flex items-center justify-between gap-2">
              <span>{{ unauthorizedText }}</span>
              <n-button text type="primary" @click="unauthorizedText = ''">我知道了</n-button>
            </div>
          </n-alert>
          <n-alert v-if="forbiddenText" class="nd-state-alert mb-4" type="error" :show-icon="false">
            <div class="flex items-center justify-between gap-2">
              <span>{{ forbiddenText }}</span>
              <n-button text type="primary" @click="forbiddenText = ''">我知道了</n-button>
            </div>
          </n-alert>
          <n-alert v-if="serverErrorText" class="nd-state-alert mb-4" type="error" :show-icon="false">
            <div class="flex items-center justify-between gap-2">
              <span>{{ serverErrorText }}</span>
              <n-button text type="primary" @click="serverErrorText = ''">关闭</n-button>
            </div>
          </n-alert>
          <n-alert v-if="showFirstLoginGuide" class="nd-state-alert mb-4" type="info" :show-icon="false">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <span>{{ firstLoginGuideText }}</span>
              <n-button text type="primary" @click="closeFirstLoginGuide">我知道了</n-button>
            </div>
          </n-alert>
          <n-alert class="nd-state-alert mb-4" type="info" :show-icon="false">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <span>{{ newbieBarText }}</span>
              <div class="flex items-center gap-2">
                <n-button text type="primary" @click="goRoleHome">返回我的首页</n-button>
                <n-button text type="primary" @click="goNewbieTask">查看新手任务</n-button>
              </div>
            </div>
          </n-alert>
          <router-view />
        </main>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { NAlert, NButton } from "naive-ui";
import SidebarNav from "@/components/layout/SidebarNav.vue";
import TopBar from "@/components/layout/TopBar.vue";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const authStore = useAuthStore();
const unauthorizedText = ref("");
const forbiddenText = ref("");
const serverErrorText = ref("");
const showFirstLoginGuide = ref(false);

const firstLoginGuideText = computed(() => {
  const role = authStore.roleKey;
  if (role === "warehouse_ops") {
    return "新手引导：先看低库存，再处理入库/出库待办。";
  }
  if (role === "cs_ops") {
    return "新手引导：先选会话，再看 AI 建议并创建工单。";
  }
  if (role === "observer") {
    return "新手引导：你是只读角色，可在总览与 Agent 历史查看关键建议。";
  }
  return "新手引导：先看经营总览，再查看审计中心与 Agent 历史。";
});

const newbieBarText = computed(() => {
  const role = authStore.roleKey;
  if (role === "warehouse_ops") return "新手入口：库存、入库、出库是你的核心工作流。";
  if (role === "cs_ops") return "新手入口：会话、工单、FAQ 是你的核心工作流。";
  if (role === "observer") return "新手入口：只读查看经营概览和风险建议。";
  return "新手入口：从经营总览进入，再做权限与流程治理。";
});

function closeFirstLoginGuide() {
  showFirstLoginGuide.value = false;
  const key = `novadepot-first-login-guide-${authStore.profile?.userId ?? "anonymous"}`;
  localStorage.setItem(key, "seen");
}

async function goRoleHome() {
  const role = authStore.roleKey;
  if (role === "warehouse_ops") {
    await router.push("/wms/inbound");
    return;
  }
  if (role === "cs_ops") {
    await router.push("/cs/workspace");
    return;
  }
  await router.push("/dashboard");
}

async function goNewbieTask() {
  const role = authStore.roleKey;
  if (role === "warehouse_ops") {
    await router.push({ path: "/wms/inventory", query: { from: "dashboard", focus: "low-stock" } });
    return;
  }
  if (role === "cs_ops") {
    await router.push({ path: "/cs/workspace", query: { guide: "ticket" } });
    return;
  }
  if (role === "observer") {
    await router.push("/agent/center");
    return;
  }
  await router.push("/dashboard");
}

function onUnauthorized(event: Event) {
  const customEvent = event as CustomEvent<{ message?: string }>;
  unauthorizedText.value = customEvent.detail?.message || "登录已失效，请重新登录。";
  authStore.logout();
  if (router.currentRoute.value.path !== "/login") {
    router.replace({ path: "/login", query: { reason: "expired" } });
  }
}

function onForbidden(event: Event) {
  const customEvent = event as CustomEvent<{ message?: string }>;
  forbiddenText.value = customEvent.detail?.message || "当前账号无权限执行该操作。";
  if (router.currentRoute.value.path !== "/access-denied") {
    router.push({
      path: "/access-denied",
      query: {
        from: router.currentRoute.value.path
      }
    });
  }
}

function onServerError(event: Event) {
  const customEvent = event as CustomEvent<{ message?: string }>;
  serverErrorText.value = customEvent.detail?.message || "系统服务暂不可用，请稍后重试。";
}

onMounted(() => {
  const key = `novadepot-first-login-guide-${authStore.profile?.userId ?? "anonymous"}`;
  showFirstLoginGuide.value = localStorage.getItem(key) !== "seen";
  window.addEventListener("novadepot:unauthorized", onUnauthorized);
  window.addEventListener("novadepot:forbidden", onForbidden);
  window.addEventListener("novadepot:server-error", onServerError);
});

onBeforeUnmount(() => {
  window.removeEventListener("novadepot:unauthorized", onUnauthorized);
  window.removeEventListener("novadepot:forbidden", onForbidden);
  window.removeEventListener("novadepot:server-error", onServerError);
});
</script>