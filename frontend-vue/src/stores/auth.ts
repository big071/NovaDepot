import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { changePasswordApi, loginApi, meApi, type AuthProfile } from "@/services/auth";

const TOKEN_KEY = "novadepot-token";

export const useAuthStore = defineStore("auth", () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? "");
  const loading = ref(false);
  const mustChangePassword = ref(false);
  const profile = ref<AuthProfile | null>(null);
  const profileLoading = ref(false);

  const isLoggedIn = computed(() => Boolean(token.value));
  const roleKey = computed(() => profile.value?.roleKey ?? "observer");
  const roleNameZh = computed(() => {
    const fromApi = profile.value?.roleNameZh?.trim();
    if (fromApi) return fromApi;
    if (roleKey.value === "admin") return "管理员";
    if (roleKey.value === "warehouse_ops") return "仓储运营";
    if (roleKey.value === "cs_ops") return "客服运营";
    return "观察员";
  });
  const permissions = computed(() => new Set(profile.value?.permissions ?? []));

  const setToken = (next: string) => {
    token.value = next;
    if (next) {
      localStorage.setItem(TOKEN_KEY, next);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  };

  const logout = () => {
    mustChangePassword.value = false;
    profile.value = null;
    setToken("");
  };

  const fetchProfile = async () => {
    if (!token.value) {
      profile.value = null;
      return null;
    }
    profileLoading.value = true;
    try {
      const data = await meApi();
      profile.value = data;
      return data;
    } catch (error) {
      profile.value = null;
      throw error;
    } finally {
      profileLoading.value = false;
    }
  };

  const ensureProfile = async () => {
    if (!token.value) return null;
    if (profile.value) return profile.value;
    try {
      return await fetchProfile();
    } catch {
      logout();
      return null;
    }
  };

  const login = async (payload: { tenantCode: string; username: string; password: string }) => {
    loading.value = true;
    try {
      const resp = await loginApi(payload);
      setToken(resp.accessToken);
      const me = await fetchProfile();
      if (!me) {
        throw new Error("登录成功后未获取到用户信息，请重新登录。");
      }
      mustChangePassword.value = Boolean(resp.mustChangePassword || me.mustChangePassword);
      return resp;
    } catch (error) {
      logout();
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const hasPermission = (permCode: string) => permissions.value.has(permCode);

  const changePassword = async (payload: { currentPassword: string; newPassword: string }) => {
    await changePasswordApi(payload);
    mustChangePassword.value = false;
    await fetchProfile();
  };

  return {
    token,
    loading,
    profileLoading,
    profile,
    roleKey,
    roleNameZh,
    isLoggedIn,
    mustChangePassword,
    login,
    fetchProfile,
    ensureProfile,
    hasPermission,
    changePassword,
    logout
  };
});
