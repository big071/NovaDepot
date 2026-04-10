import { api } from "@/services/api";

export interface AiConversation {
  id: number;
  conversationNo: string;
  scene: string;
  provider: string;
  status: string;
  startedAt: string;
}

export interface AiChatReply {
  conversationId: number;
  conversationNo: string;
  scene: string;
  provider: string;
  reply: string;
  confidence?: number;
}

export const aiApi = {
  conversations: () => api.get<AiConversation[]>("/ai/conversations"),
  chat: (payload: {
    scene: string;
    message: string;
    conversationId?: number;
    providerHint?: "rule" | "mock";
  }) => api.post<AiChatReply>("/ai/chat", payload)
};
