import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { loginApi } from "@/services/auth";

const TOKEN_KEY = "novadepot-token";

export const useAuthStore = defineStore("auth", () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? "");
  const loading = ref(false);

  const isLoggedIn = computed(() => Boolean(token.value));

  const setToken = (next: string) => {
    token.value = next;
    if (next) {
      localStorage.setItem(TOKEN_KEY, next);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  };

  const login = async (payload: { tenantCode: string; username: string; password: string }) => {
    loading.value = true;
    try {
      const resp = await loginApi(payload);
      setToken(resp.accessToken);
      return resp;
    } finally {
      loading.value = false;
    }
  };

  const logout = () => setToken("");

  return { token, loading, isLoggedIn, login, logout };
});
