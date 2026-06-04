import type { Book } from "../types/Book";

interface Props {
  books: Book[];
  onEdit: (book: Book) => void;
  onRemove: (id: number) => void;
}

export function BookList({ books, onEdit, onRemove }: Props) {
  if (books.length === 0) {
    return <p className="empty">Nenhum livro cadastrado.</p>;
  }

  return (
    <table className="books-table">
      <thead>
        <tr>
          <th>#</th>
          <th>Título</th>
          <th>Autor</th>
          <th>ISBN</th>
          <th>Ano</th>
          <th aria-label="ações" />
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
              <button type="button" onClick={() => onEdit(b)}>
                Editar
              </button>
              <button
                type="button"
                className="danger"
                onClick={() => onRemove(b.id)}
              >
                Remover
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
