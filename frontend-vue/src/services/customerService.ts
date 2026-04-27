import { api } from "@/services/api";

import type { KnowledgeRef } from "@/services/knowledge";

export interface CsSession {
  id: number;
  sessionNo: string;
  status: string;
  priority: string;
  assignedUserId?: number;
  handoffStatus?: "AI_FIRST" | "HUMAN_ASSIGNED";
}

export interface CsMessage {
  id: number;
  sessionId: number;
  sender: string;
  content: string;
  msgType: string;
  aiSuggested?: number;
  createdAt?: string;
}

export interface FaqItem {
  id: number;
  question: string;
  answer: string;
  scene: string;
}

export interface CsAiSuggestion {
  sessionId: number;
  latestCustomerText: string;
  faqHits: Array<{ faqId: number; question: string; answer: string }>;
  replyCandidates: string[];
  ticketCategorySuggestion: string;
  prioritySuggestion: string;
  sopSuggestion: string;
  basis: string[];
  knowledgeRefs?: KnowledgeRef[];
  knowledgeHit?: boolean;
  knowledgeFallbackNotice?: string;
  ruleConfigBasis?: Record<string, string>;
  humanTakeoverSuggested?: boolean;
  recommendedAction?: string;
  dataSource: string;
}

export interface SendMessageResp {
  message: CsMessage;
  autoReply?: CsMessage;
  autoReplyEnabled?: boolean;
  suggestion?: CsAiSuggestion;
}

export interface CsTicket {
  ticketId: string;
  ticketNo: string;
  sessionId: number;
  priority: string;
  content: string;
  status: string;
  assigneeUserId?: number;
  remark?: string;
  createdAt?: string;
  dataSource?: string;
  aiAutoReplied?: boolean;
  humanTakenOver?: boolean;
  nextSuggestion?: string;
}

export interface TicketPageResp {
  list: CsTicket[];
  total: number;
  pageNo: number;
  pageSize: number;
  dataSource: string;
}

export interface TicketTimelineItem {
  occurredAt?: string;
  operatorId?: string | number;
  operatorName?: string;
  action?: string;
  actionLabel?: string;
  statusFrom?: string;
  statusTo?: string;
  note?: string;
}

export interface TicketTimelineResp {
  ticket: CsTicket;
  timeline: TicketTimelineItem[];
  auditQuery?: { bizNo?: string; resourceType?: string; resourceId?: string };
}

export const csApi = {
  listSessions: () => api.get<CsSession[]>("/customer-service/sessions"),
  listMessages: (sessionId: number) => api.get<CsMessage[]>(`/customer-service/sessions/${sessionId}/messages`),
  aiSuggestions: (sessionId: number) => api.get<CsAiSuggestion>(`/customer-service/sessions/${sessionId}/ai-suggestions`),
  sendMessage: (sessionId: number, payload: { content: string; msgType?: string; sendByAi?: boolean; senderType?: "CUSTOMER" | "AGENT" | "AI"; autoReply?: boolean }) =>
    api.post<SendMessageResp>(`/customer-service/sessions/${sessionId}/messages`, payload),
  transferHuman: (sessionId: number, targetUserId: number) =>
    api.post<{ status: string }>(`/customer-service/sessions/${sessionId}/actions/transfer-human`, { targetUserId }),
  createTicket: (payload: { sessionId: number; priority: string; content: string }) =>
    api.post<{ ticketNo: string; status: string }>("/customer-service/tickets", payload),
  updateTicketStatus: (ticketId: string, status: string, note?: string) =>
    api.put<CsTicket>(`/customer-service/tickets/${ticketId}/status`, { status, note }),
  updateTicketOwner: (ticketId: string, assigneeUserId: number) =>
    api.put<CsTicket>(`/customer-service/tickets/${ticketId}/owner`, { assigneeUserId }),
  updateTicketRemark: (ticketId: string, remark: string) =>
    api.put<CsTicket>(`/customer-service/tickets/${ticketId}/remark`, { remark }),
  ticketTimeline: (ticketId: string) =>
    api.get<TicketTimelineResp>(`/customer-service/tickets/${ticketId}/timeline`),
  listTickets: (sessionId?: number, pageNo = 1, pageSize = 10) =>
    api.get<TicketPageResp>("/customer-service/tickets", { sessionId, pageNo, pageSize }),
  faq: (keyword?: string, scene?: string) => api.get<FaqItem[]>("/customer-service/faq", { keyword, scene }),
  updateFaq: (id: number, payload: { question: string; answer: string; scene?: string }) =>
    api.put<FaqItem>(`/customer-service/faq/${id}`, payload)
};
