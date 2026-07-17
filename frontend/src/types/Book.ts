export interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  publishedYear: number;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface BookRequest {
  title: string;
  author: string;
  isbn: string;
  publishedYear: number;
}

export interface ApiError {
  error: string;
  message: string;
  fields?: Array<{ field: string; message: string }>;
}

export type HistoryOperation = "CREATED" | "UPDATED" | "DELETED";

export interface BookHistory {
  id: number;
  bookId: number;
  operation: HistoryOperation;
  title: string;
  author: string;
  isbn: string;
  publishedYear: number;
  bookVersion: number;
  changedAt: string;
}
