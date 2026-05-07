import { api } from "@/services/api";

export interface BackupRecord {
  id: string;
  backupNo: string;
  fileName?: string;
  filePath?: string;
  fileSize?: number;
  checksum?: string;
  status: string;
  startedAt: string;
  finishedAt?: string;
  errorMessage?: string;
}

export const backupApi = {
  list: () => api.get<BackupRecord[]>("/backups"),
  run: () => api.post<{ id: string; backupNo: string; status: string; fileName?: string; errorMessage?: string }>("/backups/actions/run")
};
