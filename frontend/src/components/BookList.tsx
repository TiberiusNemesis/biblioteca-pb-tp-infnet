import type { Book } from "../types/Book";

interface Props {
  books: Book[];
  onHistory: (id: number) => void;
  onEdit: (book: Book) => void;
  onRemove: (id: number) => void;
}

export function BookList({ books, onHistory, onEdit, onRemove }: Props) {
  if (books.length === 0) {
    return <p className="empty">No books registered.</p>;
  }

  return (
    <table className="books-table">
      <thead>
        <tr>
          <th>#</th>
          <th>Title</th>
          <th>Author</th>
          <th>ISBN</th>
          <th>Year</th>
          <th aria-label="Actions" />
        </tr>
      </thead>
      <tbody>
        {books.map((b) => (
          <tr key={b.id}>
            <td>{b.id}</td>
            <td>{b.title}</td>
            <td>{b.author}</td>
            <td>
              <code>{b.isbn}</code>
            </td>
            <td>{b.publishedYear}</td>
            <td className="actions">
              <button type="button" onClick={() => onHistory(b.id)}>
                History
              </button>
              <button type="button" onClick={() => onEdit(b)}>
                Edit
              </button>
              <button
                type="button"
                className="danger"
                onClick={() => onRemove(b.id)}
              >
                Remove
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
