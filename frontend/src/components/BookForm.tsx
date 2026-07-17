import { useEffect, useState } from "react";
import type { Book, BookRequest } from "../types/Book";
import {
  createBookFormState,
  toBookRequest,
  updatePublicationYear,
  type BookFormState,
} from "./bookFormState";

interface Props {
  editing?: Book;
  onSubmit: (body: BookRequest) => void;
  onCancelEdit?: () => void;
  disabled?: boolean;
}

export function BookForm({ editing, onSubmit, onCancelEdit, disabled }: Props) {
  const [form, setForm] = useState<BookFormState>(() =>
    createBookFormState(),
  );

  useEffect(() => {
    setForm(createBookFormState(editing));
  }, [editing]);

  const update = <K extends keyof BookFormState>(
    key: K,
    value: BookFormState[K],
  ) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (form.publishedYear === "") return;

    onSubmit(toBookRequest(form));
    if (!editing) setForm(createBookFormState());
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
          onChange={(e) =>
            setForm((prev) =>
              updatePublicationYear(prev, e.target.value),
            )
          }
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
