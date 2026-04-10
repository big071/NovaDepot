<template>
  <n-config-provider :theme="naiveTheme">
    <n-message-provider>
      <router-view />
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { darkTheme, NConfigProvider, NMessageProvider } from "naive-ui";
import { storeToRefs } from "pinia";
import { useThemeStore } from "@/stores/theme";

const themeStore = useThemeStore();
const { theme } = storeToRefs(themeStore);

const naiveTheme = computed(() => (theme.value === "dark" ? darkTheme : null));
</script>
