import { api } from "@/services/api";

export interface KnowledgeRef {
  type: "FAQ" | "SOP" | "RULE";
  code?: string;
  title: string;
  scene?: string;
  matchedTags?: string[];
  reason?: string;
  nextAction?: string;
}

export interface FaqKnowledge {
  id: string;
  faqCode: string;
  question: string;
  answer: string;
  tags: string;
  scene: string;
  priority: number;
  enabled: number;
  reviewStatus: "DRAFT" | "APPROVED";
  sourceType?: string;
  sourceRefId?: string;
}

export interface SopKnowledge {
  id: string;
  sopCode: string;
  title: string;
  scene: string;
  steps: string;
  risks: string;
  reviewChecks: string;
  tags: string;
  priority: number;
  enabled: number;
  reviewStatus: "DRAFT" | "APPROVED";
  sourceType?: string;
  sourceRefId?: string;
}

export interface RuleConfig {
  id: string;
  configKey: string;
  configName: string;
  configValue: string;
  valueType: string;
  scene: string;
  remark: string;
  enabled: number;
}

export const knowledgeApi = {
  listFaqs: (query?: { keyword?: string; scene?: string; status?: string }) => api.get<FaqKnowledge[]>("/knowledge/faqs", query),
  createFaq: (payload: Partial<FaqKnowledge>) => api.post<FaqKnowledge>("/knowledge/faqs", payload),
  updateFaq: (id: string, payload: Partial<FaqKnowledge>) => api.put<FaqKnowledge>(`/knowledge/faqs/${id}`, payload),
  confirmFaq: (id: string) => api.post<FaqKnowledge>(`/knowledge/faqs/${id}/confirm`),
  enableFaq: (id: string) => api.post<FaqKnowledge>(`/knowledge/faqs/${id}/enable`),
  disableFaq: (id: string) => api.post<FaqKnowledge>(`/knowledge/faqs/${id}/disable`),
  listSops: (query?: { keyword?: string; scene?: string; status?: string }) => api.get<SopKnowledge[]>("/knowledge/sops", query),
  createSop: (payload: Partial<SopKnowledge>) => api.post<SopKnowledge>("/knowledge/sops", payload),
  updateSop: (id: string, payload: Partial<SopKnowledge>) => api.put<SopKnowledge>(`/knowledge/sops/${id}`, payload),
  confirmSop: (id: string) => api.post<SopKnowledge>(`/knowledge/sops/${id}/confirm`),
  enableSop: (id: string) => api.post<SopKnowledge>(`/knowledge/sops/${id}/enable`),
  disableSop: (id: string) => api.post<SopKnowledge>(`/knowledge/sops/${id}/disable`),
  listRules: (scene?: string) => api.get<RuleConfig[]>("/knowledge/rules", { scene }),
  updateRule: (configKey: string, payload: Partial<RuleConfig>) => api.put<RuleConfig>(`/knowledge/rules/${configKey}`, payload),
  draftFaqFromTicket: (ticketId: string) => api.post<FaqKnowledge>(`/knowledge/drafts/from-ticket/${ticketId}/faq`),
  draftSopFromTicket: (ticketId: string) => api.post<SopKnowledge>(`/knowledge/drafts/from-ticket/${ticketId}/sop`)
};
