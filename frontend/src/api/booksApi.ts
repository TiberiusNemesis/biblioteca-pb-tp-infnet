import type { ApiError, Book, BookRequest } from "../types/Book";

const BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080/api";

async function jsonRequest<T>(input: string, init?: RequestInit): Promise<T> {
  const res = await fetch(input, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  const payload = text ? JSON.parse(text) : null;

  if (!res.ok) {
    const err = payload as ApiError | null;
    const msg = err?.message ?? `Request failed (${res.status})`;
    const detail =
      err?.fields?.map((f) => `${f.field}: ${f.message}`).join("; ") ?? "";
    throw new Error(detail ? `${msg} — ${detail}` : msg);
  }

  return payload as T;
}

export const booksApi = {
  list: () => jsonRequest<Book[]>(`${BASE}/books`),

  get: (id: number) => jsonRequest<Book>(`${BASE}/books/${id}`),

  create: (body: BookRequest) =>
    jsonRequest<Book>(`${BASE}/books`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  update: (id: number, body: BookRequest) =>
    jsonRequest<Book>(`${BASE}/books/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),

  remove: (id: number) =>
    jsonRequest<void>(`${BASE}/books/${id}`, { method: "DELETE" }),
};
