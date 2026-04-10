import { api } from "@/services/api";

export interface CsSession {
  id: number;
  sessionNo: string;
  status: string;
  priority: string;
}

export interface CsMessage {
  id: number;
  sessionId: number;
  sender: string;
  content: string;
  msgType: string;
  createdAt?: string;
}

export interface FaqItem {
  id: number;
  question: string;
  answer: string;
  scene: string;
}

export const csApi = {
  listSessions: () => api.get<CsSession[]>("/customer-service/sessions"),
  listMessages: (sessionId: number) => api.get<CsMessage[]>(`/customer-service/sessions/${sessionId}/messages`),
  sendMessage: (sessionId: number, payload: { content: string; msgType?: string; sendByAi?: boolean }) =>
    api.post<CsMessage>(`/customer-service/sessions/${sessionId}/messages`, payload),
  transferHuman: (sessionId: number, targetUserId: number) =>
    api.post<{ status: string }>(`/customer-service/sessions/${sessionId}/actions/transfer-human`, { targetUserId }),
  createTicket: (payload: { sessionId: number; priority: string; content: string }) =>
    api.post<{ ticketNo: string; status: string }>("/customer-service/tickets", payload),
  faq: (keyword?: string, scene?: string) => api.get<FaqItem[]>("/customer-service/faq", { keyword, scene })
};
