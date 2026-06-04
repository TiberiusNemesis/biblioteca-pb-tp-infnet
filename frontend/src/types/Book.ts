export interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  publishedYear: number;
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
