const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";
type Primitive = string | number | boolean;
type QueryValue = Primitive | null | undefined;

function getToken() {
  return localStorage.getItem("novadepot-token") ?? "";
}

interface ApiEnvelope<T> {
  code: string;
  message: string;
  data: T;
  traceId?: string;
}

export class ApiRequestError extends Error {
  status: number;
  code: string;
  traceId?: string;

  constructor(message: string, status: number, code = "UNKNOWN", traceId?: string) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
    this.code = code;
    this.traceId = traceId;
  }
}

function emitEvent(eventName: string, detail: Record<string, unknown>) {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new CustomEvent(eventName, { detail }));
}

function emitForbidden(message: string, traceId?: string) {
  emitEvent("novadepot:forbidden", { message, traceId });
}

function emitUnauthorized(message: string, traceId?: string) {
  emitEvent("novadepot:unauthorized", { message, traceId });
}

function emitServerError(message: string, traceId?: string) {
  emitEvent("novadepot:server-error", { message, traceId });
}

function withQuery(path: string, query?: Record<string, QueryValue>) {
  if (!query) return path;
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== "") {
      params.append(key, String(value));
    }
  });
  const queryString = params.toString();
  return queryString ? `${path}?${queryString}` : path;
}

function handleGlobalError(status: number, code: string, traceId?: string) {
  if (status === 401 || code === "AUTH-0001") {
    emitUnauthorized("登录已失效，请重新登录。", traceId);
    return;
  }
  if (status === 403 || code === "AUTH-0002" || code === "AUTH-0003") {
    emitForbidden("当前账号无权限执行该操作，请联系管理员授权。", traceId);
    return;
  }
  if (status >= 500 || code === "SYS-9999") {
    emitServerError("系统服务暂时不可用，请稍后重试。", traceId);
  }
}

function parseEnvelopeSafe<T>(text: string): ApiEnvelope<T> | null {
  if (!text || !text.trim()) {
    return null;
  }
  try {
    return JSON.parse(text) as ApiEnvelope<T>;
  } catch {
    return null;
  }
}

export async function request<T>(
  path: string,
  method: HttpMethod = "GET",
  body?: unknown,
  query?: Record<string, QueryValue>
): Promise<T> {
  const target = `${API_BASE_URL}${withQuery(path, query)}`;
  const hasStringBody = typeof body === "string";
  const res = await fetch(target, {
    method,
    headers: {
      "Content-Type": hasStringBody ? "text/plain;charset=UTF-8" : "application/json",
      Authorization: `Bearer ${getToken()}`
    },
    body: body ? (hasStringBody ? body : JSON.stringify(body)) : undefined
  });

  const text = await res.text();
  const json = parseEnvelopeSafe<T>(text);

  if (!res.ok) {
    const message = json?.message ?? (res.status === 403 ? "当前请求被拒绝（可能需要先修改密码）。" : `Request failed: ${res.status}`);
    const code = json?.code ?? "HTTP_ERROR";
    const traceId = json?.traceId;
    handleGlobalError(res.status, code, traceId);
    throw new ApiRequestError(message, res.status, code, traceId);
  }

  if (!json) {
    throw new ApiRequestError("响应格式非法", res.status, "INVALID_JSON");
  }

  if (json.code !== "0") {
    handleGlobalError(res.status, json.code, json.traceId);
    throw new ApiRequestError(json.message || "Request failed", res.status, json.code, json.traceId);
  }

  return json.data;
}

export const api = {
  get: <T>(path: string, query?: Record<string, QueryValue>) => request<T>(path, "GET", undefined, query),
  post: <T>(path: string, body?: unknown) => request<T>(path, "POST", body),
  put: <T>(path: string, body?: unknown) => request<T>(path, "PUT", body),
  del: <T>(path: string) => request<T>(path, "DELETE")
};
