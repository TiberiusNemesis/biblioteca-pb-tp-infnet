import type { BookHistory } from "../types/Book";

interface Props {
  bookId: number;
  history: BookHistory[];
  loading: boolean;
  onClose: () => void;
}

export function BookHistoryPanel({ bookId, history, loading, onClose }: Props) {
  return (
    <section className="history-panel" aria-labelledby="history-title">
      <div className="history-header">
        <h2 id="history-title">History for book #{bookId}</h2>
        <button type="button" onClick={onClose} aria-label="Close history">
          Close
        </button>
      </div>

      {loading && <p className="history-meta">Loading history…</p>}

      {!loading && history.length === 0 && (
        <p className="history-meta">No history records found.</p>
      )}

      {!loading && history.length > 0 && (
        <ol className="history-list">
          {history.map((entry) => (
            <li className="history-entry" key={entry.id}>
              <div className="history-entry-heading">
                <strong>{entry.operation}</strong>
                <span>Version {entry.bookVersion}</span>
                <time dateTime={entry.changedAt}>
                  {new Date(entry.changedAt).toLocaleString()}
                </time>
              </div>
              <p>{entry.title}</p>
              <p className="history-meta">
                {entry.author} · {entry.publishedYear} · <code>{entry.isbn}</code>
              </p>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
