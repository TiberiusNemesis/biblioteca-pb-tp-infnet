import { useEffect, useState } from "react";
import type { Book, BookRequest } from "../types/Book";

interface Props {
  editing?: Book;
  onSubmit: (body: BookRequest) => void;
  onCancelEdit?: () => void;
  disabled?: boolean;
}

const EMPTY: BookRequest = {
  title: "",
  author: "",
  isbn: "",
  publishedYear: new Date().getFullYear(),
};

export function BookForm({ editing, onSubmit, onCancelEdit, disabled }: Props) {
  const [form, setForm] = useState<BookRequest>(EMPTY);

  useEffect(() => {
    if (editing) {
      setForm({
        title: editing.title,
        author: editing.author,
        isbn: editing.isbn,
        publishedYear: editing.publishedYear,
      });
    } else {
      setForm(EMPTY);
    }
  }, [editing]);

  const update = <K extends keyof BookRequest>(key: K, value: BookRequest[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(form);
    if (!editing) setForm(EMPTY);
  };

  return (
    <form className="book-form" onSubmit={submit}>
      <h2>{editing ? `Edit book #${editing.id}` : "New book"}</h2>

      <label>
        Title
        <input
          required
          value={form.title}
          onChange={(e) => update("title", e.target.value)}
        />
      </label>

      <label>
        Author
        <input
          required
          value={form.author}
          onChange={(e) => update("author", e.target.value)}
        />
      </label>

      <label>
        ISBN
        <input
          required
          placeholder="978-XX-XXXX-XXX-X"
          value={form.isbn}
          onChange={(e) => update("isbn", e.target.value)}
        />
      </label>

      <label>
        Publication year
        <input
          type="number"
          required
          min={1450}
          max={new Date().getFullYear()}
          value={form.publishedYear}
          onChange={(e) => update("publishedYear", Number(e.target.value))}
        />
      </label>

      <div className="form-actions">
        <button type="submit" disabled={disabled}>
          {editing ? "Save" : "Add"}
        </button>
        {editing && onCancelEdit && (
          <button type="button" onClick={onCancelEdit}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
