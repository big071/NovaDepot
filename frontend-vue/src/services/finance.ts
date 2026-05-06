import { api } from "@/services/api";

export interface Payable {
  id: string;
  payableNo: string;
  sourceOrderNo: string;
  partnerId: string;
  warehouseId: string;
  totalAmount: number;
  paidAmount: number;
  balanceAmount: number;
  status: string;
  remark?: string;
}

export interface Receivable {
  id: string;
  receivableNo: string;
  sourceOrderNo: string;
  partnerId: string;
  warehouseId: string;
  totalAmount: number;
  receivedAmount: number;
  balanceAmount: number;
  status: string;
  remark?: string;
}

export interface FinanceRegistrationPayload {
  amount: number;
  paidAt?: string;
  method?: string;
  remark?: string;
}

export const financeApi = {
  listPayables: (query?: { status?: string }) => api.get<Payable[]>("/finance/payables", query),
  listReceivables: (query?: { status?: string }) => api.get<Receivable[]>("/finance/receivables", query),
  registerPayment: (id: string, payload: FinanceRegistrationPayload) =>
    api.post<{ id: string; status: string; balanceAmount: number }>(`/finance/payables/${id}/payments`, payload),
  registerReceipt: (id: string, payload: FinanceRegistrationPayload) =>
    api.post<{ id: string; status: string; balanceAmount: number }>(`/finance/receivables/${id}/receipts`, payload)
};
