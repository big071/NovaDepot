import { API_BASE_URL, api, getToken } from "@/services/api";

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
  lastActiveAt?: string;
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
  status?: "PENDING" | "STREAMING" | "COMPLETED" | "FAILED" | "STOPPED";
  createdAt?: string;
}

export type AiStreamEvent =
  | { event: "meta"; data: Record<string, unknown> }
  | { event: "token"; data: { content?: string } }
  | { event: "status"; data: Record<string, unknown> }
  | { event: "done"; data: Record<string, unknown> }
  | { event: "error"; data: { message?: string } };

function parseSseBlock(block: string): AiStreamEvent | null {
  const lines = block.split(/\r?\n/);
  const eventLine = lines.find((line) => line.startsWith("event:"));
  const dataLines = lines.filter((line) => line.startsWith("data:"));
  if (!eventLine || dataLines.length === 0) return null;
  const event = eventLine.slice("event:".length).trim() as AiStreamEvent["event"];
  const rawData = dataLines.map((line) => line.slice("data:".length).trim()).join("\n");
  try {
    return { event, data: JSON.parse(rawData) } as AiStreamEvent;
  } catch {
    return null;
  }
}

export async function streamAiChat(
  payload: {
    scene: string;
    message: string;
    conversationNo?: string;
    providerHint?: "rule" | "mock" | "deepseek-chat" | "deepseek-reasoner";
  },
  requestId: string,
  signal: AbortSignal,
  onEvent: (event: AiStreamEvent) => void
) {
  const res = await fetch(`${API_BASE_URL}/ai/chat/stream?requestId=${encodeURIComponent(requestId)}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${getToken()}`
    },
    body: JSON.stringify(payload),
    signal
  });
  if (!res.ok || !res.body) {
    throw new Error(`流式请求失败：${res.status}`);
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const blocks = buffer.split(/\r?\n\r?\n/);
    buffer = blocks.pop() ?? "";
    for (const block of blocks) {
      const parsed = parseSseBlock(block);
      if (parsed) onEvent(parsed);
    }
  }
  if (buffer.trim()) {
    const parsed = parseSseBlock(buffer);
    if (parsed) onEvent(parsed);
  }
}

export const aiApi = {
  conversations: () => api.get<AiConversation[]>("/ai/conversations"),
  createConversation: (scene?: string) =>
    api.post<AiConversation>(`/ai/conversations${scene ? `?scene=${encodeURIComponent(scene)}` : ""}`),
  archiveConversation: (id: string | number) => api.post<AiConversation>(`/ai/conversations/${id}/archive`),
  messagesByNo: (conversationNo: string) =>
    api.get<AiMessage[]>(`/ai/conversations/by-no/${encodeURIComponent(conversationNo)}/messages`),
  chat: (payload: {
    scene: string;
    message: string;
    conversationNo?: string;
    providerHint?: "rule" | "mock" | "deepseek-chat" | "deepseek-reasoner";
  }) => api.post<AiChatReply>("/ai/chat", payload),
  stopStream: (requestId: string) =>
    api.post<{ requestId: string; stopped: boolean }>(`/ai/chat/stream/${encodeURIComponent(requestId)}/stop`),
  config: () => api.get<AiConfig>("/ai/config"),
  usageLogs: (conversationId?: number, limit?: number) =>
    api.get<AiUsageLog[]>("/ai/usage-logs", { conversationId: conversationId ?? undefined, limit: limit ?? 100 })
};
