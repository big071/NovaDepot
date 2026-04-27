import { api } from "@/services/api";

export interface AgentTaskItem {
  taskCode: string;
  taskName: string;
  description: string;
  intro?: string;
  readData?: string[];
  output?: string;
  params?: AgentTaskParam[];
}

export interface AgentTaskParam {
  key: string;
  label: string;
  description: string;
  type: "number" | "text" | "date";
  defaultValue?: string | number;
}

export interface AgentStepItem {
  stepNo: number;
  phase: string;
  name: string;
  status: string;
  detail?: string;
  durationMs?: number;
  snapshot?: unknown;
  timestamp?: string;
}

export interface AgentRunDetail {
  id: string;
  taskCode: string;
  taskName: string;
  status: string;
  startedAt?: string;
  finishedAt?: string;
  errorMessage?: string;
  target?: Record<string, unknown>;
  steps: AgentStepItem[];
  result: Record<string, unknown>;
}

export interface AgentRunListItem {
  id: string;
  taskCode: string;
  taskName: string;
  status: string;
  startedAt?: string;
  finishedAt?: string;
  errorMessage?: string;
}

export interface AgentRunPageResp {
  list: AgentRunListItem[];
  total: number;
  pageNo: number;
  pageSize: number;
}

export const agentApi = {
  listTasks: () => api.get<AgentTaskItem[]>("/agent/tasks"),
  executeTask: (taskCode: string, target: Record<string, unknown>) =>
    api.post<AgentRunDetail>(`/agent/tasks/${taskCode}/execute`, { target }),
  listRuns: (query: Record<string, string | number | null | undefined>) =>
    api.get<AgentRunPageResp>("/agent/runs", query),
  getRunDetail: (id: string) => api.get<AgentRunDetail>(`/agent/runs/${id}`)
};
