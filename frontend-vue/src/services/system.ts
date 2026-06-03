import { API_BASE_URL, api, getToken } from "@/services/api";

export interface UserItem {
  id: number;
  username: string;
  realName: string;
  tenantId: number;
}

export interface PermissionItem {
  id: string | number;
  permCode: string;
  permName: string;
  resource?: string;
  action?: string;
  status?: string;
}

export interface RoleItem {
  id: string | number;
  tenantId?: string | number;
  roleCode: string;
  roleName: string;
  dataScope: "ALL" | "WAREHOUSE" | "SELF";
  status: "ACTIVE" | "DISABLED";
  builtIn?: boolean;
  permissionCount?: number;
  permissionIds?: Array<string | number>;
  permissions?: PermissionItem[];
}

export interface RoleSavePayload {
  roleCode: string;
  roleName: string;
  dataScope: "ALL" | "WAREHOUSE" | "SELF";
  status: "ACTIVE" | "DISABLED";
  permissionIds: Array<string | number>;
}

export interface AuditLogItem {
  id: string;
  module: string;
  action: string;
  resourceType: string;
  resourceId: string;
  bizNo?: string;
  operatorId?: number;
  operatorName?: string;
  occurredAt?: string;
}

export interface AuditLogDiffItem {
  field: string;
  before: unknown;
  after: unknown;
}

export interface AuditLogDetail extends AuditLogItem {
  beforeJson?: string;
  afterJson?: string;
  beforeObject?: Record<string, unknown>;
  afterObject?: Record<string, unknown>;
  diff?: AuditLogDiffItem[];
  ip?: string;
  userAgent?: string;
}

export interface AuditLogPageResp {
  list: AuditLogItem[];
  total: number;
  pageNo: number;
  pageSize: number;
}

export interface AuditLogQuery {
  [key: string]: string | number | boolean | null | undefined;
  pageNo?: number;
  pageSize?: number;
  module?: string;
  action?: string;
  resourceType?: string;
  resourceId?: string;
  bizNo?: string;
  operatorId?: number;
  operatorKeyword?: string;
  onlyFailed?: boolean;
  dateFrom?: string;
  dateTo?: string;
}

export const systemApi = {
  listUsers: () => api.get<UserItem[]>("/users"),
  listRoles: () => api.get<RoleItem[]>("/roles"),
  getRole: (id: string | number) => api.get<RoleItem>(`/roles/${id}`),
  createRole: (payload: RoleSavePayload) => api.post<{ id: string | number; roleCode: string }>("/roles", payload),
  updateRole: (id: string | number, payload: RoleSavePayload) => api.put<{ id: string | number; roleCode: string }>(`/roles/${id}`, payload),
  listPermissions: () => api.get<PermissionItem[]>("/permissions"),
  listAuditLogs: (query: AuditLogQuery) => api.get<AuditLogPageResp>("/audit-logs", query),
  getAuditLogDetail: (id: string | number) => api.get<AuditLogDetail>(`/audit-logs/${id}`),
  exportAuditLogs: (query: AuditLogQuery) => downloadCsv("/audit-logs/export", query, "audit-logs.csv")
};

function queryString(query: AuditLogQuery) {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== "") {
      params.append(key, String(value));
    }
  });
  return params.toString();
}

async function downloadCsv(path: string, query: AuditLogQuery, fileName: string) {
  const qs = queryString(query);
  const res = await fetch(`${API_BASE_URL}${path}${qs ? `?${qs}` : ""}`, {
    headers: { Authorization: `Bearer ${getToken()}` }
  });
  if (!res.ok) {
    throw new Error(`Download failed: ${res.status}`);
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
