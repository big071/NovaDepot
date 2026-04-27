import { api } from "@/services/api";

export interface DashboardMetrics {
  totalSku: number;
  todayInbound: number;
  todayOutbound: number;
  lowStockCount: number;
}

export interface DashboardTodoResp {
  roleKey: string;
  todos: Record<string, unknown>;
  metrics: DashboardMetrics;
}

export const reportsApi = {
  dashboard: () => api.get<DashboardMetrics>("/reports/dashboard"),
  dashboardTodos: () => api.get<DashboardTodoResp>("/reports/dashboard/todos")
};
