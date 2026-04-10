import { api } from "@/services/api";

export interface DashboardMetrics {
  totalSku: number;
  todayInbound: number;
  todayOutbound: number;
  lowStockCount: number;
}

export const reportsApi = {
  dashboard: () => api.get<DashboardMetrics>("/reports/dashboard")
};
