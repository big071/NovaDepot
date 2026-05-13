<template>
  <header class="sticky top-0 z-20 border-b border-border/80 bg-surface/85 px-4 py-3 backdrop-blur lg:px-6">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-xs uppercase tracking-[0.2em] text-text-secondary">NovaDepot SaaS</p>
        <h2 class="text-sm font-semibold">{{ authStore.roleNameZh }}工作台</h2>
      </div>
      <div class="flex items-center gap-2">
        <n-button v-if="authStore.hasPermission('NOTIFY_READ')" secondary @click="router.push('/notifications')">
          通知
          <span v-if="unreadCount > 0" class="ml-1 rounded-full bg-danger px-1.5 py-0.5 text-xs text-white">{{ unreadCount }}</span>
        </n-button>
        <span class="hidden rounded-full border border-border px-2 py-1 text-xs text-text-secondary xl:inline-flex">
          {{ authStore.profile?.realName || authStore.profile?.username || "当前用户" }}
        </span>
        <n-button secondary @click="showPwdModal = true">修改密码</n-button>
        <n-button secondary type="primary" @click="themeStore.toggleTheme()">
          {{ themeStore.isDark ? "浅色" : "深色" }}
        </n-button>
        <n-button quaternary @click="onLogout">退出</n-button>
      </div>
    </div>
  </header>

  <n-modal v-model:show="showPwdModal" preset="card" title="修改密码" class="max-w-lg" :mask-closable="false">
    <div class="space-y-3">
      <n-alert v-if="authStore.mustChangePassword" type="warning" :show-icon="false">
        首次登录或管理员重置后，必须先修改密码。
      </n-alert>
      <n-input v-model:value="form.currentPassword" type="password" show-password-on="click" placeholder="当前密码" />
      <n-input v-model:value="form.newPassword" type="password" show-password-on="click" placeholder="新密码" />
      <n-input v-model:value="form.confirmPassword" type="password" show-password-on="click" placeholder="确认新密码" />
      <n-alert v-if="errorText" type="error" :show-icon="false">{{ errorText }}</n-alert>
    </div>
    <template #footer>
      <div class="flex justify-end gap-2">
        <n-button :disabled="authStore.mustChangePassword" @click="showPwdModal = false">取消</n-button>
        <n-button type="primary" :loading="submitting" @click="onChangePassword">保存</n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { NAlert, NButton, NInput, NModal, useMessage } from "naive-ui";
import { useThemeStore } from "@/stores/theme";
import { useAuthStore } from "@/stores/auth";
import { useRouter } from "vue-router";
import { notificationsApi } from "@/services/notifications";

const themeStore = useThemeStore();
const authStore = useAuthStore();
const router = useRouter();
const message = useMessage();

const showPwdModal = ref(false);
const submitting = ref(false);
const errorText = ref("");
const unreadCount = ref(0);
let unreadTimer: number | undefined;
const form = reactive({
  currentPassword: "",
  newPassword: "",
  confirmPassword: ""
});

watch(
  () => authStore.mustChangePassword,
  (next) => {
    if (next) {
      showPwdModal.value = true;
    }
  },
  { immediate: true }
);

onMounted(() => {
  if (authStore.mustChangePassword) {
    showPwdModal.value = true;
  }
  refreshUnreadCount();
  unreadTimer = window.setInterval(refreshUnreadCount, 60000);
  window.addEventListener("novadepot:notifications-refresh", refreshUnreadCount);
});

onUnmounted(() => {
  if (unreadTimer) {
    window.clearInterval(unreadTimer);
  }
  window.removeEventListener("novadepot:notifications-refresh", refreshUnreadCount);
});

async function refreshUnreadCount() {
  if (!authStore.hasPermission("NOTIFY_READ")) return;
  try {
    unreadCount.value = await notificationsApi.unreadCount();
  } catch {
    unreadCount.value = 0;
  }
}

async function onChangePassword() {
  errorText.value = "";
  if (!form.currentPassword || !form.newPassword || !form.confirmPassword) {
    errorText.value = "请完整输入密码字段";
    return;
  }
  if (form.newPassword !== form.confirmPassword) {
    errorText.value = "两次新密码不一致";
    return;
  }

  submitting.value = true;
  try {
    await authStore.changePassword({
      currentPassword: form.currentPassword,
      newPassword: form.newPassword
    });
    form.currentPassword = "";
    form.newPassword = "";
    form.confirmPassword = "";
    showPwdModal.value = false;
    message.success("密码修改成功");
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "密码修改失败";
  } finally {
    submitting.value = false;
  }
}

function onLogout() {
  authStore.logout();
  router.push("/login");
}
</script>
