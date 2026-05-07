import { api } from "@/services/api";

import type { KnowledgeRef } from "@/services/knowledge";

export interface AiConfig {
  defaultProvider: string;
  deepseekEnabled: boolean;
  deepseekBaseUrl: string;
  deepseekChatModel: string;
  deepseekReasonerModel: string;
  deepseekApiKeyMasked: string;
  paidEnabled: boolean;
}

export interface AiUsageLog {
  id: number;
  conversationId: number;
  provider: string;
  model: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  latencyMs: number;
  success: boolean;
  errorMessage: string;
  createdAt: string;
}

export interface AiConversation {
  id: string;
  conversationNo: string;
  scene: string;
  provider: string;
  status: string;
  startedAt: string;
}

export interface AiChatReply {
  conversationId: string;
  conversationNo: string;
  scene: string;
  provider: string;
  reply: string;
  confidence?: number;
  taskRouted?: boolean;
  taskCode?: string;
  taskName?: string;
  taskRun?: {
    id?: string;
    status?: string;
    steps?: Array<Record<string, unknown>>;
    result?: Record<string, unknown>;
  };
  executionSteps?: Array<Record<string, unknown>>;
  executionBasis?: Record<string, unknown>;
  executionResult?: Record<string, unknown>;
  resultView?: Record<string, unknown>;
  knowledgeRefs?: KnowledgeRef[];
  knowledgeHit?: boolean;
  knowledgeFallbackNotice?: string;
}

export interface AiMessage {
  id: string;
  conversationId: string;
  role: "USER" | "ASSISTANT";
  content: string;
  createdAt?: string;
}

export const aiApi = {
  conversations: () => api.get<AiConversation[]>("/ai/conversations"),
  messagesByNo: (conversationNo: string) =>
    api.get<AiMessage[]>(`/ai/conversations/by-no/${encodeURIComponent(conversationNo)}/messages`),
  chat: (payload: {
    scene: string;
    message: string;
    conversationNo?: string;
    providerHint?: "rule" | "mock" | "deepseek-chat" | "deepseek-reasoner";
  }) => api.post<AiChatReply>("/ai/chat", payload),
  config: () => api.get<AiConfig>("/ai/config"),
  usageLogs: (conversationId?: number, limit?: number) =>
    api.get<AiUsageLog[]>("/ai/usage-logs", { conversationId: conversationId ?? undefined, limit: limit ?? 100 })
};
