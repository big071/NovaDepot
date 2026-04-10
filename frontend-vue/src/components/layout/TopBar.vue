<template>
  <header class="sticky top-0 z-20 border-b border-border/80 bg-surface/85 px-4 py-3 backdrop-blur lg:px-6">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-xs uppercase tracking-[0.2em] text-text-secondary">NovaDepot SaaS</p>
        <h2 class="text-sm font-semibold">Vue 3 Migration Workspace</h2>
      </div>
      <div class="flex items-center gap-2">
        <n-button secondary type="primary" @click="themeStore.toggleTheme()">
          {{ themeStore.isDark ? "浅色" : "深色" }}
        </n-button>
        <n-button quaternary @click="onLogout">退出</n-button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { NButton } from "naive-ui";
import { useThemeStore } from "@/stores/theme";
import { useAuthStore } from "@/stores/auth";
import { useRouter } from "vue-router";

const themeStore = useThemeStore();
const authStore = useAuthStore();
const router = useRouter();

function onLogout() {
  authStore.logout();
  router.push("/login");
}
</script>
