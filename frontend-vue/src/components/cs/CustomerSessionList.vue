<template>
  <article class="nd-table-shell">
    <div class="nd-table-head"><h3 class="nd-section-title">浼氳瘽鍒楄〃</h3></div>
    <div class="nd-table-body space-y-2">
      <n-empty v-if="!loading && sessions.length === 0" class="nd-empty-shell" description="鏆傛棤浼氳瘽" />
      <button
        v-for="item in sessions"
        :key="item.id"
        class="w-full rounded-xl border p-3 text-left transition"
        :class="activeSessionId === item.id ? 'border-primary bg-primary/10' : 'border-border bg-bg/50 hover:border-primary/40'"
        @click="$emit('select-session', item.id)"
      >
        <div class="flex items-center justify-between">
          <p class="text-sm font-medium">{{ item.sessionNo }}</p>
          <n-tag :bordered="false" size="small" :type="item.priority === 'HIGH' ? 'error' : 'warning'">{{ item.priority }}</n-tag>
        </div>
        <p class="mt-1 text-xs text-text-secondary">鐘舵€侊細{{ item.status }}</p>
        <p class="mt-1 text-xs text-text-secondary">澶勭悊妯″紡锛歿{ item.handoffStatus === 'HUMAN_ASSIGNED' ? '浜哄伐鎺ョ涓? : 'AI浼樺厛' }}</p>
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { NEmpty, NTag } from "naive-ui";
import type { CsSession } from "@/services/customerService";

defineProps<{
  sessions: CsSession[];
  activeSessionId: number | null;
  loading: boolean;
}>();

defineEmits<{
  "select-session": [id: number];
}>();
</script>
