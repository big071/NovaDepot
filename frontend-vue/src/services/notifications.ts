import { api } from "@/services/api";

export interface NotificationItem {
  id: number | string;
  notifyType: string;
  bizType: string;
  bizNo: string;
  title: string;
  content: string;
  severity?: "INFO" | "WARNING" | "ERROR";
  readFlag: boolean | number;
  sentAt: string;
  readAt?: string;
  jumpPath?: string;
}

export interface NotificationPage {
  list: NotificationItem[];
  total: number;
  pageNo: number;
  pageSize: number;
}

export const notificationsApi = {
  list: (query: { unreadOnly?: boolean; pageNo?: number; pageSize?: number }) =>
    api.get<NotificationPage>("/notifications", query),
  detail: (id: number | string) => api.get<NotificationItem>(`/notifications/${id}`),
  unreadCount: async () => (await api.get<{ unreadCount: number }>("/notifications/unread-count")).unreadCount,
  markRead: (id: number | string) => api.post<NotificationItem>(`/notifications/${id}/read`),
  markAllRead: () => api.post<{ unreadCount: number }>("/notifications/read-all")
};
