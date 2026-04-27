import { api } from "@/services/api";

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  mustChangePassword?: boolean;
}

export interface AuthProfile {
  userId: number;
  username: string;
  realName?: string;
  tenantId: number;
  roleCodes: string[];
  permissions: string[];
  roleKey: "admin" | "warehouse_ops" | "cs_ops" | "observer";
  roleNameZh: string;
  mustChangePassword?: boolean;
}

export function loginApi(payload: { tenantCode: string; username: string; password: string }) {
  return api.post<LoginResponse>("/auth/login", payload);
}

export function changePasswordApi(payload: { currentPassword: string; newPassword: string }) {
  return api.post<{ success: boolean }>("/auth/change-password", payload);
}

export function resetPasswordApi(userId: number, payload: { newPassword: string }) {
  return api.post<{ success: boolean }>(`/auth/users/${userId}/reset-password`, payload);
}

export function meApi() {
  return api.get<AuthProfile>("/auth/me");
}
