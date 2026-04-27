import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url))
    }
  },
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes("node_modules")) return undefined;
          if (id.includes("echarts") || id.includes("zrender") || id.includes("vue-echarts")) return "vendor-echarts";
          if (id.includes("naive-ui") || id.includes("vooks") || id.includes("vueuc")) return "vendor-naive";
          if (id.includes("pinia") || id.includes("vue-router") || id.includes("/vue/")) return "vendor-core";
          return "vendor-misc";
        }
      }
    }
  },
  server: {
    host: "0.0.0.0",
    port: 3100
  }
});
