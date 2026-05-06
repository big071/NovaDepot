<template>
  <n-tag :bordered="false" :type="type" size="small">{{ label }}</n-tag>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { NTag } from "naive-ui";

const props = defineProps<{ status?: string }>();

const label = computed(() => props.status || "-");
const type = computed(() => {
  switch ((props.status || "").toUpperCase()) {
    case "DRAFT":
      return "default";
    case "CONFIRMED":
    case "PARTIAL_RECEIVED":
    case "PARTIAL_SHIPPED":
      return "info";
    case "COMPLETED":
    case "POSTED":
    case "FULLY_RECEIVED":
    case "FULLY_SHIPPED":
    case "SHIPPED":
      return "success";
    case "CANCELLED":
    case "CANCELED":
      return "error";
    default:
      return "warning";
  }
});
</script>
