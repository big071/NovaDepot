import { defineStore } from "pinia";
import { computed, ref } from "vue";

export type ThemeMode = "light" | "dark";

export const useThemeStore = defineStore("theme", () => {
  const theme = ref<ThemeMode>("light");

  const isDark = computed(() => theme.value === "dark");

  const setTheme = (next: ThemeMode) => {
    theme.value = next;
    document.documentElement.classList.toggle("dark", next === "dark");
    localStorage.setItem("novadepot-theme", next);
  };

  const toggleTheme = () => {
    setTheme(theme.value === "light" ? "dark" : "light");
  };

  const initTheme = () => {
    const saved = localStorage.getItem("novadepot-theme");
    if (saved === "dark" || saved === "light") {
      setTheme(saved);
      return;
    }
    const preferDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    setTheme(preferDark ? "dark" : "light");
  };

  return { theme, isDark, setTheme, toggleTheme, initTheme };
});
