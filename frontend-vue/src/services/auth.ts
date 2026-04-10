import { api } from "@/services/api";

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export function loginApi(payload: { tenantCode: string; username: string; password: string }) {
  return api.post<LoginResponse>("/auth/login", payload);
}
