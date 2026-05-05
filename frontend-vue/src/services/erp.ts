import { api } from "@/services/api";

export interface Partner {
  id: string;
  partnerCode: string;
  partnerName: string;
  partnerType: "SUPPLIER" | "CUSTOMER" | "BOTH";
  contactName?: string;
  phone?: string;
  address?: string;
  status?: string;
  remark?: string;
}

export interface ErpOrder {
  id: string;
  purchaseNo?: string;
  salesNo?: string;
  status: string;
  partnerId: string;
  warehouseId: string;
  totalAmount: number;
  expectedArrivalDate?: string;
  deliveryDate?: string;
  remark?: string;
}

export interface ErpOrderItem {
  id: string;
  lineNo: number;
  productId: string;
  orderQty: number;
  unitPrice: number;
  receivedQty?: number;
  shippedQty?: number;
}

export interface ErpOrderDetail {
  order: ErpOrder;
  items: ErpOrderItem[];
}

export interface OrderPayload {
  partnerId: number;
  warehouseId: number;
  expectedArrivalDate?: string;
  deliveryDate?: string;
  remark?: string;
  items: Array<{ productId: number; orderQty: number; unitPrice: number }>;
}

export const erpApi = {
  listPartners: (query?: { keyword?: string; partnerType?: string }) => api.get<Partner[]>("/partners", query),
  getPartner: (id: string) => api.get<Partner>(`/partners/${id}`),
  createPartner: (payload: Partial<Partner>) => api.post<{ id: string }>("/partners", payload),
  updatePartner: (id: string, payload: Partial<Partner>) => api.put<{ id: string }>(`/partners/${id}`, payload),
  enablePartner: (id: string) => api.post<{ id: string; status: string }>(`/partners/${id}/actions/enable`),
  disablePartner: (id: string) => api.post<{ id: string; status: string }>(`/partners/${id}/actions/disable`),

  listPurchaseOrders: (query?: { status?: string; partnerId?: string }) => api.get<ErpOrder[]>("/purchase-orders", query),
  getPurchaseOrder: (id: string) => api.get<ErpOrderDetail>(`/purchase-orders/${id}`),
  createPurchaseOrder: (payload: OrderPayload) => api.post<{ id: string; purchaseNo: string; status: string }>("/purchase-orders", payload),
  updatePurchaseOrder: (id: string, payload: OrderPayload) => api.put<{ id: string; status: string }>(`/purchase-orders/${id}`, payload),
  confirmPurchaseOrder: (id: string) => api.post<{ id: string; status: string }>(`/purchase-orders/${id}/actions/confirm`),
  cancelPurchaseOrder: (id: string) => api.post<{ id: string; status: string }>(`/purchase-orders/${id}/actions/cancel`),

  listSalesOrders: (query?: { status?: string; partnerId?: string }) => api.get<ErpOrder[]>("/sales-orders", query),
  getSalesOrder: (id: string) => api.get<ErpOrderDetail>(`/sales-orders/${id}`),
  createSalesOrder: (payload: OrderPayload) => api.post<{ id: string; salesNo: string; status: string }>("/sales-orders", payload),
  updateSalesOrder: (id: string, payload: OrderPayload) => api.put<{ id: string; status: string }>(`/sales-orders/${id}`, payload),
  confirmSalesOrder: (id: string) => api.post<{ id: string; status: string }>(`/sales-orders/${id}/actions/confirm`),
  cancelSalesOrder: (id: string) => api.post<{ id: string; status: string }>(`/sales-orders/${id}/actions/cancel`)
};
