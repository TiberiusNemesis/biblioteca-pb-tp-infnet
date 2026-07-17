import type { Book, BookRequest } from "../types/Book";

export interface BookFormState {
  title: string;
  author: string;
  isbn: string;
  publishedYear: string;
}

export function createBookFormState(
  book?: Book,
  currentYear = new Date().getFullYear(),
): BookFormState {
  return {
    title: book?.title ?? "",
    author: book?.author ?? "",
    isbn: book?.isbn ?? "",
    publishedYear: String(book?.publishedYear ?? currentYear),
  };
}

export function updatePublicationYear(
  form: BookFormState,
  publishedYear: string,
): BookFormState {
  return { ...form, publishedYear };
}

export function toBookRequest(form: BookFormState): BookRequest {
  if (form.publishedYear === "") {
    throw new Error("Publication year is required");
  }

  return {
    ...form,
    publishedYear: Number(form.publishedYear),
  };
}
