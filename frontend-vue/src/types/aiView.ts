import type { DataTableColumns } from "naive-ui";
import type { AiConversation, AiToolCallView } from "@/services/ai";
import type { KnowledgeRef } from "@/services/knowledge";

export type AiMessageStatus = "PENDING" | "STREAMING" | "COMPLETED" | "FAILED" | "STOPPED";

export type ToolCallStatus = "CALLING" | "SUCCESS" | "DENIED" | "EMPTY" | "FAILED";

export type ToolCallMessage = AiToolCallView & {
  status?: ToolCallStatus;
};

export type ChatMessage = {
  role: "user" | "assistant";
  content: string;
  status?: AiMessageStatus;
  toolCalls?: ToolCallMessage[];
  validationWarnings?: string[];
  toolLimitReached?: boolean;
};

export type SummaryCard = {
  title: string;
  value: string;
};

export type RenderedSection = {
  title: string;
  content: string[];
  items: string[];
  badge?: string;
  badgeType?: "default" | "error" | "info" | "success" | "warning";
};

export type AiTaskRunInfo = {
  id?: string;
  taskCode?: string;
  taskName?: string;
  executionBasis?: Record<string, unknown>;
  executionResult?: Record<string, unknown>;
  resultView?: Record<string, unknown>;
};

export type AiProviderStatusContext = {
  activeConversation: AiConversation | null;
  activeConversationNo: string | null;
  scene: string;
  sceneOptions: Array<{ label: string; value: string }>;
  aiConfigWarning: string;
  lastKnowledgeRefs: KnowledgeRef[];
  lastKnowledgeNotice: string;
  recommendedQuestions: string[];
  lastSuccessText: string;
  streamStatusText: string;
  taskRunInfo: AiTaskRunInfo | null;
  taskSummaryCards: SummaryCard[];
  taskBasisList: string[];
  taskActionList: string[];
  taskResultColumns: DataTableColumns<Record<string, unknown>>;
  taskResultRows: Array<Record<string, unknown>>;
};
