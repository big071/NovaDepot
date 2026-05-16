import { API_BASE_URL, api, getToken } from "@/services/api";

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

export interface InventoryTurnoverRow {
  productId: number;
  productName: string;
  outboundQty: number;
  availableQty: number;
  turnoverRate: number;
}

export interface InoutSummaryRow {
  period: string;
  inboundCount: number;
  outboundCount: number;
  netCount: number;
}

export interface PurchaseSalesSummaryRow {
  type: string;
  count: number;
  amount: number;
}

export interface TicketEfficiencyRow {
  assigneeId: number;
  ticketCount: number;
  closedCount: number;
  closeRate: number;
}

export interface ReportResp<T> {
  reportName: string;
  dateFrom: string;
  dateTo: string;
  rows: T[];
  total: number;
}

export interface ReportQuery {
  [key: string]: string | number | undefined;
  dateFrom?: string;
  dateTo?: string;
  warehouseId?: number | string;
  partnerId?: number | string;
  assigneeId?: number | string;
  grain?: "DAY" | "WEEK";
}

function queryString(query: ReportQuery) {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.append(key, String(value));
    }
  });
  return params.toString();
}

async function downloadCsv(path: string, query: ReportQuery, fileName: string) {
  const qs = queryString(query);
  const res = await fetch(`${API_BASE_URL}${path}${qs ? `?${qs}` : ""}`, {
    headers: { Authorization: `Bearer ${getToken()}` }
  });
  if (!res.ok) {
    throw new Error(`CSV 导出失败：${res.status}`);
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export const reportsApi = {
  dashboard: () => api.get<DashboardMetrics>("/reports/dashboard"),
  dashboardTodos: () => api.get<DashboardTodoResp>("/reports/dashboard/todos"),
  inventoryTurnover: (query: ReportQuery) => api.get<ReportResp<InventoryTurnoverRow>>("/reports/inventory-turnover", query),
  inoutSummary: (query: ReportQuery) => api.get<ReportResp<InoutSummaryRow>>("/reports/inout-summary", query),
  purchaseSalesSummary: (query: ReportQuery) => api.get<ReportResp<PurchaseSalesSummaryRow>>("/reports/purchase-sales-summary", query),
  ticketEfficiency: (query: ReportQuery) => api.get<ReportResp<TicketEfficiencyRow>>("/reports/ticket-efficiency", query),
  exportInventoryTurnover: (query: ReportQuery) => downloadCsv("/reports/inventory-turnover/export", query, "inventory-turnover.csv"),
  exportInoutSummary: (query: ReportQuery) => downloadCsv("/reports/inout-summary/export", query, "inout-summary.csv"),
  exportPurchaseSalesSummary: (query: ReportQuery) => downloadCsv("/reports/purchase-sales-summary/export", query, "purchase-sales-summary.csv"),
  exportTicketEfficiency: (query: ReportQuery) => downloadCsv("/reports/ticket-efficiency/export", query, "ticket-efficiency.csv")
};
