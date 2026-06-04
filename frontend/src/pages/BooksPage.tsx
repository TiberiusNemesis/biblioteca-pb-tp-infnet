import { useCallback, useEffect, useState } from "react";
import { booksApi } from "../api/booksApi";
import { BookForm } from "../components/BookForm";
import { BookList } from "../components/BookList";
import { ErrorBanner } from "../components/ErrorBanner";
import type { Book, BookRequest } from "../types/Book";

export function BooksPage() {
  const [books, setBooks] = useState<Book[]>([]);
  const [editing, setEditing] = useState<Book | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const reload = useCallback(async () => {
    try {
      setBooks(await booksApi.list());
    } catch (e) {
      setError((e as Error).message);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const handleSubmit = async (body: BookRequest) => {
    setBusy(true);
    setError(null);
    try {
      if (editing) {
        const saved = await booksApi.update(editing.id, body);
        setBooks((prev) => prev.map((b) => (b.id === saved.id ? saved : b)));
        setEditing(undefined);
      } else {
        const created = await booksApi.create(body);
        setBooks((prev) => [...prev, created]);
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const handleRemove = async (id: number) => {
    if (!confirm(`Remover o livro #${id}?`)) return;
    setError(null);
    try {
      await booksApi.remove(id);
      setBooks((prev) => prev.filter((b) => b.id !== id));
      if (editing?.id === id) setEditing(undefined);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <section className="books-page">
      <ErrorBanner message={error} onDismiss={() => setError(null)} />

      <BookForm
        editing={editing}
        onSubmit={handleSubmit}
        onCancelEdit={() => setEditing(undefined)}
        disabled={busy}
      />

      <BookList books={books} onEdit={setEditing} onRemove={handleRemove} />
    </section>
  );
}
