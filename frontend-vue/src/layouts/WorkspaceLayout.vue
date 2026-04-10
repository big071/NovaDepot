<template>
  <div class="min-h-screen bg-bg text-text-primary">
    <div class="flex min-h-screen">
      <SidebarNav />
      <section class="flex min-h-screen flex-1 flex-col">
        <TopBar />
        <main class="flex-1 p-4 lg:p-6">
          <router-view />
        </main>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import SidebarNav from "@/components/layout/SidebarNav.vue";
import TopBar from "@/components/layout/TopBar.vue";
</script>
