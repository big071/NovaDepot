<template>
  <div class="flex min-h-screen items-center justify-center px-4">
    <div class="w-full max-w-md rounded-3xl border border-border bg-surface p-8 shadow-card">
      <p class="text-sm text-text-secondary">NovaDepot</p>
      <h1 class="mt-1 text-2xl font-semibold">登录系统（Vue）</h1>

      <n-alert v-if="errorText" class="mt-4" type="error" :show-icon="false">{{ errorText }}</n-alert>

      <div class="mt-5 space-y-3">
        <n-input v-model:value="form.tenantCode" placeholder="租户编码" />
        <n-input v-model:value="form.username" placeholder="用户名" />
        <n-input v-model:value="form.password" type="password" show-password-on="click" placeholder="密码" />
        <n-button type="primary" block :loading="authStore.loading" @click="onLogin">登录</n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { NAlert, NButton, NInput, useMessage } from "naive-ui";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const authStore = useAuthStore();
const message = useMessage();

const errorText = ref("");
const form = reactive({
  tenantCode: "default",
  username: "admin",
  password: "123456"
});

async function onLogin() {
  errorText.value = "";
  if (!form.tenantCode.trim() || !form.username.trim() || !form.password.trim()) {
    message.warning("请完整输入租户、用户名和密码");
    return;
  }
  try {
    await authStore.login(form);
    router.push("/dashboard");
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "登录失败";
  }
}
</script>
