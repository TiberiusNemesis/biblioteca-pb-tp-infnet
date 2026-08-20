import type { ApiError } from "../types/Book";
import type { CreateLoanRequest, Loan } from "../types/Loan";

const BASE =
  import.meta.env.VITE_LOAN_API_URL ?? "http://localhost:8081/api";

async function jsonRequest<T>(input: string, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const error = payload as ApiError | null;
    const message = error?.message ?? `Request failed (${response.status})`;
    const details =
      error?.fields?.map((field) => `${field.field}: ${field.message}`).join("; ") ??
      "";
    throw new Error(details ? `${message} — ${details}` : message);
  }

  return payload as T;
}

export const loansApi = {
  list: () => jsonRequest<Loan[]>(`${BASE}/loans`),

  create: (body: CreateLoanRequest) =>
    jsonRequest<Loan>(`${BASE}/loans`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  returnLoan: (id: number) =>
    jsonRequest<Loan>(`${BASE}/loans/${id}/return`, {
      method: "PATCH",
    }),
};
