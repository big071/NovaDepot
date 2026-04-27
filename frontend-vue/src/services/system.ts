import { api } from "@/services/api";

export interface UserItem {
  id: number;
  username: string;
  realName: string;
  tenantId: number;
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
  listAuditLogs: (query: AuditLogQuery) => api.get<AuditLogPageResp>("/audit-logs", query),
  getAuditLogDetail: (id: string | number) => api.get<AuditLogDetail>(`/audit-logs/${id}`)
};
