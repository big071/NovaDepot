import { api } from "@/services/api";

export interface Product {
  id: number;
  productCode: string;
  productName: string;
  barcode?: string;
  status?: string;
}

export interface Warehouse {
  id: number;
  warehouseCode: string;
  warehouseName: string;
  warehouseType?: string;
  status?: string;
}

export interface Location {
  id: number;
  warehouseId: number;
  locationCode: string;
  locationName: string;
  locationType?: string;
  capacityQty?: number;
  status?: string;
}

export interface InventoryItem {
  id: number;
  warehouseId: number;
  locationId: number;
  productId: number;
  availableQty: number;
  lockedQty: number;
  inTransitQty: number;
}

export interface InboundOrder {
  id: number;
  inboundNo: string;
  warehouseId: number;
  supplierId?: number;
  status: string;
}

export interface OutboundOrder {
  id: number;
  outboundNo: string;
  warehouseId: number;
  customerId?: number;
  status: string;
}

export const wmsApi = {
  listProducts: () => api.get<Product[]>("/products"),
  createProduct: (payload: {
    productCode: string;
    productName: string;
    categoryId: number;
    unitId: number;
    barcode?: string;
  }) => api.post<{ id: number }>("/products", payload),

  listWarehouses: () => api.get<Warehouse[]>("/warehouses"),
  createWarehouse: (payload: {
    warehouseCode: string;
    warehouseName: string;
    warehouseType?: string;
    address?: string;
  }) => api.post<{ id: number }>("/warehouses", payload),

  listLocations: (warehouseId?: number) => api.get<Location[]>("/locations", { warehouseId }),
  createLocation: (payload: {
    warehouseId: number;
    locationCode: string;
    locationName: string;
    locationType?: string;
    capacityQty?: number;
  }) => api.post<{ id: number }>("/locations", payload),

  listInventory: () => api.get<InventoryItem[]>("/inventory"),
  listLowStockAlerts: () => api.get<InventoryItem[]>("/inventory/alerts/low-stock"),

  listInboundOrders: () => api.get<InboundOrder[]>("/inbound-orders"),
  createInboundOrder: (payload: {
    warehouseId: number;
    supplierId?: number;
    items: Array<{ productId: number; locationId: number; qty: number }>;
  }) => api.post<{ id: number; inboundNo: string }>("/inbound-orders", payload),
  approveInboundOrder: (id: number) => api.post<{ status: string }>(`/inbound-orders/${id}/actions/approve`),
  postInboundOrder: (id: number) => api.post<{ status: string }>(`/inbound-orders/${id}/actions/post`),

  listOutboundOrders: () => api.get<OutboundOrder[]>("/outbound-orders"),
  createOutboundOrder: (payload: {
    warehouseId: number;
    customerId?: number;
    items: Array<{ productId: number; locationId: number; qty: number }>;
  }) => api.post<{ id: number; outboundNo: string }>("/outbound-orders", payload),
  approveOutboundOrder: (id: number) => api.post<{ status: string }>(`/outbound-orders/${id}/actions/approve`),
  shipOutboundOrder: (id: number) => api.post<{ status: string }>(`/outbound-orders/${id}/actions/ship`)
};
