import { api } from "@/services/api";

interface QueryOptions {
  force?: boolean;
}

interface CacheEntry<T> {
  expireAt: number;
  value: T;
}

const listCache = new Map<string, CacheEntry<unknown>>();

function getCacheKey(scope: string, payload?: string) {
  return payload ? `${scope}:${payload}` : scope;
}

function readCache<T>(key: string) {
  const cached = listCache.get(key) as CacheEntry<T> | undefined;
  if (!cached) return null;
  if (Date.now() > cached.expireAt) {
    listCache.delete(key);
    return null;
  }
  return cached.value;
}

function writeCache<T>(key: string, value: T, ttlMs = 20_000) {
  listCache.set(key, { value, expireAt: Date.now() + ttlMs });
}

function invalidatePrefix(prefix: string) {
  Array.from(listCache.keys()).forEach((key) => {
    if (key.startsWith(prefix)) listCache.delete(key);
  });
}

async function withCache<T>(key: string, query: () => Promise<T>, options?: QueryOptions, ttlMs = 20_000) {
  if (!options?.force) {
    const cached = readCache<T>(key);
    if (cached) return cached;
  }
  const result = await query();
  writeCache(key, result, ttlMs);
  return result;
}

export interface Product {
  id: string;
  productCode: string;
  productName: string;
  barcode?: string;
  status?: string;
}

export interface Warehouse {
  id: string;
  warehouseCode: string;
  warehouseName: string;
  warehouseType?: string;
  status?: string;
}

export interface Location {
  id: string;
  warehouseId: string;
  locationCode: string;
  locationName: string;
  locationType?: string;
  capacityQty?: number;
  status?: string;
}

export interface InventoryItem {
  id: string;
  warehouseId: string;
  locationId: string;
  productId: string;
  availableQty: number;
  lockedQty: number;
  inTransitQty: number;
}

export interface InventoryTransaction {
  id: string;
  txnNo: string;
  bizType: string;
  bizNo: string;
  warehouseId: string;
  locationId?: string;
  productId: string;
  changeQty: number;
  beforeQty: number;
  afterQty: number;
  occurredAt?: string;
}

export interface InboundOrder {
  id: string;
  inboundNo: string;
  warehouseId: string;
  supplierId?: number;
  status: string;
  sourceType?: string;
  sourceOrderId?: string;
  sourceOrderNo?: string;
  createdBy?: string;
  createdAt?: string;
}

export interface OutboundOrder {
  id: string;
  outboundNo: string;
  warehouseId: string;
  customerId?: number;
  status: string;
  sourceType?: string;
  sourceOrderId?: string;
  sourceOrderNo?: string;
  createdBy?: string;
  createdAt?: string;
}

export interface InboundOrderItem {
  id: string;
  lineNo: number;
  productId: string;
  locationId: string;
  planQty: number;
  receivedQty: number;
  qualifiedQty: number;
}

export interface OutboundOrderItem {
  id: string;
  lineNo: number;
  productId: string;
  locationId: string;
  planQty: number;
  pickedQty: number;
  shippedQty: number;
}

export interface OrderTimelineItem {
  occurredAt?: string;
  operatorId?: string | number;
  operatorName?: string;
  action?: string;
  actionLabel?: string;
  statusFrom?: string;
  statusTo?: string;
  note?: string;
}

export interface OrderDetail<TOrder, TItem> {
  order: TOrder;
  items: TItem[];
  timeline: OrderTimelineItem[];
  auditQuery?: { bizNo?: string; resourceType?: string; resourceId?: string };
}

export const wmsApi = {
  listProducts: (options?: QueryOptions) =>
    withCache(getCacheKey("products"), () => api.get<Product[]>("/products"), options, 25_000),
  getProductDetail: (id: string) => api.get<Product>(`/products/${id}`),
  getProductDetailByCode: (productCode: string) => api.get<Product>(`/products/code/${encodeURIComponent(productCode)}`),
  createProduct: async (payload: {
    productCode: string;
    productName: string;
    categoryId: number;
    unitId: number;
    barcode?: string;
  }) => {
    const result = await api.post<{ id: number }>("/products", payload);
    invalidatePrefix("products");
    return result;
  },
  updateProduct: async (id: string, payload: {
    productCode: string;
    productName: string;
    categoryId: number;
    unitId: number;
    barcode?: string;
  }) => {
    const result = await api.put<{ id: number }>(`/products/${id}`, payload);
    invalidatePrefix("products");
    return result;
  },
  exportProductImportTemplate: () => api.get<string>("/products/import/template"),
  getProductImportErrorReport: (reportId: string) => api.get<string>(`/products/import/errors/${reportId}`),
  importProducts: (csvContent: string) => api.post<Record<string, unknown>>("/products/import", csvContent),

  listWarehouses: (options?: QueryOptions) =>
    withCache(getCacheKey("warehouses"), () => api.get<Warehouse[]>("/warehouses"), options, 30_000),
  getWarehouseDetail: (id: string) => api.get<Warehouse>(`/warehouses/${id}`),
  getWarehouseDetailByCode: (warehouseCode: string) =>
    api.get<Warehouse>(`/warehouses/code/${encodeURIComponent(warehouseCode)}`),
  createWarehouse: async (payload: {
    warehouseCode: string;
    warehouseName: string;
    warehouseType?: string;
    address?: string;
  }) => {
    const result = await api.post<{ id: number }>("/warehouses", payload);
    invalidatePrefix("warehouses");
    invalidatePrefix("locations");
    return result;
  },
  updateWarehouse: async (id: string, payload: {
    warehouseCode: string;
    warehouseName: string;
    warehouseType?: string;
    address?: string;
  }) => {
    const result = await api.put<{ id: number }>(`/warehouses/${id}`, payload);
    invalidatePrefix("warehouses");
    return result;
  },

  listLocations: (warehouseId?: string, options?: QueryOptions) =>
    withCache(
      getCacheKey("locations", warehouseId || "all"),
      () => api.get<Location[]>("/locations", { warehouseId }),
      options,
      30_000
    ),
  getLocationDetail: (id: string) => api.get<Location>(`/locations/${id}`),
  getLocationDetailByCode: (locationCode: string) => api.get<Location>(`/locations/code/${encodeURIComponent(locationCode)}`),
  createLocation: async (payload: {
    warehouseId: string;
    locationCode: string;
    locationName: string;
    locationType?: string;
    capacityQty?: number;
  }) => {
    const result = await api.post<{ id: number }>("/locations", payload);
    invalidatePrefix("locations");
    return result;
  },
  updateLocation: async (id: string, payload: {
    warehouseId: string;
    locationCode: string;
    locationName: string;
    locationType?: string;
    capacityQty?: number;
  }) => {
    const result = await api.put<{ id: number }>(`/locations/${id}`, payload);
    invalidatePrefix("locations");
    return result;
  },

  listInventory: (options?: QueryOptions) =>
    withCache(getCacheKey("inventory"), () => api.get<InventoryItem[]>("/inventory"), options, 20_000),
  listLowStockAlerts: (options?: QueryOptions) =>
    withCache(getCacheKey("inventory-alerts"), () => api.get<InventoryItem[]>("/inventory/alerts/low-stock"), options, 15_000),
  listInventoryTransactions: (options?: QueryOptions) =>
    withCache(
      getCacheKey("inventory-transactions"),
      () => api.get<InventoryTransaction[]>("/inventory/transactions"),
      options,
      15_000
    ),
  importInventory: (csvContent: string) => api.post<Record<string, unknown>>("/inventory/import", csvContent),
  inventoryExportFields: () => api.get<string[]>("/inventory/export/fields"),

  listInboundOrders: (options?: QueryOptions) =>
    withCache(getCacheKey("inbound-orders"), () => api.get<InboundOrder[]>("/inbound-orders"), options, 20_000),
  listInboundOrderItems: (id: string, options?: QueryOptions) =>
    withCache(getCacheKey("inbound-items", id), () => api.get<InboundOrderItem[]>(`/inbound-orders/${id}/items`), options, 20_000),
  getInboundOrderDetail: (id: string, options?: QueryOptions) =>
    withCache(getCacheKey("inbound-detail", id), () => api.get<OrderDetail<InboundOrder, InboundOrderItem>>(`/inbound-orders/${id}/detail`), options, 15_000),
  createInboundOrder: async (payload: {
    warehouseId: string;
    supplierId?: number;
    items: Array<{ productId: string; locationId: string; qty: number }>;
  }) => {
    const result = await api.post<{ id: string; inboundNo: string }>("/inbound-orders", payload);
    invalidatePrefix("inbound-orders");
    return result;
  },
  updateInboundOrder: async (id: string, payload: {
    warehouseId: string;
    supplierId?: number;
    items: Array<{ productId: string; locationId: string; qty: number }>;
  }) => {
    const result = await api.put<{ id: string; status: string }>(`/inbound-orders/${id}`, payload);
    invalidatePrefix("inbound-orders");
    invalidatePrefix(getCacheKey("inbound-items", id));
    return result;
  },
  submitInboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/inbound-orders/${id}/actions/submit`, { note });
    invalidatePrefix("inbound-orders");
    return result;
  },
  withdrawInboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/inbound-orders/${id}/actions/withdraw`, { note });
    invalidatePrefix("inbound-orders");
    return result;
  },
  cancelInboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/inbound-orders/${id}/actions/cancel`, { note });
    invalidatePrefix("inbound-orders");
    return result;
  },
  unapproveInboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/inbound-orders/${id}/actions/unapprove`, { note });
    invalidatePrefix("inbound-orders");
    return result;
  },
  approveInboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/inbound-orders/${id}/actions/approve`, { note });
    invalidatePrefix("inbound-orders");
    invalidatePrefix(getCacheKey("inbound-items", id));
    return result;
  },
  rejectInboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/inbound-orders/${id}/actions/reject`, { note });
    invalidatePrefix("inbound-orders");
    return result;
  },
  postInboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/inbound-orders/${id}/actions/post`, { note });
    invalidatePrefix("inbound-orders");
    invalidatePrefix(getCacheKey("inbound-items", id));
    invalidatePrefix(getCacheKey("inbound-detail", id));
    invalidatePrefix("inventory");
    invalidatePrefix("inventory-alerts");
    invalidatePrefix("inventory-transactions");
    return result;
  },

  listOutboundOrders: (options?: QueryOptions) =>
    withCache(getCacheKey("outbound-orders"), () => api.get<OutboundOrder[]>("/outbound-orders"), options, 20_000),
  listOutboundOrderItems: (id: string, options?: QueryOptions) =>
    withCache(getCacheKey("outbound-items", id), () => api.get<OutboundOrderItem[]>(`/outbound-orders/${id}/items`), options, 20_000),
  getOutboundOrderDetail: (id: string, options?: QueryOptions) =>
    withCache(getCacheKey("outbound-detail", id), () => api.get<OrderDetail<OutboundOrder, OutboundOrderItem>>(`/outbound-orders/${id}/detail`), options, 15_000),
  createOutboundOrder: async (payload: {
    warehouseId: string;
    customerId?: number;
    items: Array<{ productId: string; locationId: string; qty: number }>;
  }) => {
    const result = await api.post<{ id: string; outboundNo: string }>("/outbound-orders", payload);
    invalidatePrefix("outbound-orders");
    return result;
  },
  updateOutboundOrder: async (id: string, payload: {
    warehouseId: string;
    customerId?: number;
    items: Array<{ productId: string; locationId: string; qty: number }>;
  }) => {
    const result = await api.put<{ id: string; status: string }>(`/outbound-orders/${id}`, payload);
    invalidatePrefix("outbound-orders");
    invalidatePrefix(getCacheKey("outbound-items", id));
    return result;
  },
  submitOutboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/outbound-orders/${id}/actions/submit`, { note });
    invalidatePrefix("outbound-orders");
    return result;
  },
  withdrawOutboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/outbound-orders/${id}/actions/withdraw`, { note });
    invalidatePrefix("outbound-orders");
    return result;
  },
  cancelOutboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/outbound-orders/${id}/actions/cancel`, { note });
    invalidatePrefix("outbound-orders");
    return result;
  },
  unapproveOutboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/outbound-orders/${id}/actions/unapprove`, { note });
    invalidatePrefix("outbound-orders");
    return result;
  },
  approveOutboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/outbound-orders/${id}/actions/approve`, { note });
    invalidatePrefix("outbound-orders");
    invalidatePrefix(getCacheKey("outbound-items", id));
    return result;
  },
  rejectOutboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/outbound-orders/${id}/actions/reject`, { note });
    invalidatePrefix("outbound-orders");
    return result;
  },
  shipOutboundOrder: async (id: string, note?: string) => {
    const result = await api.post<{ status: string }>(`/outbound-orders/${id}/actions/ship`, { note });
    invalidatePrefix("outbound-orders");
    invalidatePrefix(getCacheKey("outbound-items", id));
    invalidatePrefix(getCacheKey("outbound-detail", id));
    invalidatePrefix("inventory");
    invalidatePrefix("inventory-alerts");
    invalidatePrefix("inventory-transactions");
    return result;
  }
};
