const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:18080/api/v1";

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

export async function request<T>(
  path: string,
  method: HttpMethod = "GET",
  body?: unknown,
  query?: Record<string, QueryValue>
): Promise<T> {
  const target = `${API_BASE_URL}${withQuery(path, query)}`;
  const res = await fetch(target, {
    method,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${getToken()}`
    },
    body: body ? JSON.stringify(body) : undefined
  });

  const text = await res.text();
  const json = (text ? JSON.parse(text) : null) as ApiEnvelope<T> | null;

  if (!res.ok) {
    throw new Error(json?.message ?? `Request failed: ${res.status}`);
  }

  if (!json) {
    throw new Error("Empty response body");
  }

  return json.data;
}

export const api = {
  get: <T>(path: string, query?: Record<string, QueryValue>) => request<T>(path, "GET", undefined, query),
  post: <T>(path: string, body?: unknown) => request<T>(path, "POST", body),
  put: <T>(path: string, body?: unknown) => request<T>(path, "PUT", body),
  del: <T>(path: string) => request<T>(path, "DELETE")
};
