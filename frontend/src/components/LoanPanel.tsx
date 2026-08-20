import { useEffect, useState } from "react";
import type { Book } from "../types/Book";
import type { CreateLoanRequest, Loan } from "../types/Loan";
import {
  createLoanFormState,
  toCreateLoanRequest,
  updateLoanDueDate,
  type LoanFormState,
} from "./loanFormState";

interface Props {
  selectedBook?: Book;
  loans: Loan[];
  loading: boolean;
  busy: boolean;
  onSubmit: (request: CreateLoanRequest) => void;
  onReturn: (id: number) => void;
  onCancelBorrow: () => void;
}

export function LoanPanel({
  selectedBook,
  loans,
  loading,
  busy,
  onSubmit,
  onReturn,
  onCancelBorrow,
}: Props) {
  const [form, setForm] = useState<LoanFormState | null>(null);

  useEffect(() => {
    setForm(selectedBook ? createLoanFormState(selectedBook.id) : null);
  }, [selectedBook]);

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    if (form) onSubmit(toCreateLoanRequest(form));
  };

  return (
    <section className="loan-panel" aria-labelledby="loans-title">
      <div className="loan-header">
        <div>
          <h2 id="loans-title">Loans</h2>
          <p>Borrow catalog books and record their return.</p>
        </div>
      </div>

      {selectedBook && form && (
        <form className="loan-form" onSubmit={submit}>
          <h3>Borrow “{selectedBook.title}”</h3>
          <label>
            Borrower name
            <input
              required
              maxLength={120}
              value={form.borrowerName}
              onChange={(event) =>
                setForm((current) =>
                  current
                    ? { ...current, borrowerName: event.target.value }
                    : current,
                )
              }
            />
          </label>
          <label>
            Due date
            <input
              type="date"
              required
              value={form.dueDate}
              onChange={(event) =>
                setForm((current) =>
                  current
                    ? updateLoanDueDate(current, event.target.value)
                    : current,
                )
              }
            />
          </label>
          <div className="form-actions">
            <button type="submit" disabled={busy}>
              Confirm loan
            </button>
            <button type="button" onClick={onCancelBorrow} disabled={busy}>
              Cancel
            </button>
          </div>
        </form>
      )}

      {loading && <p className="loan-meta">Loading loans…</p>}

      {!loading && loans.length === 0 && (
        <p className="empty">No loans registered.</p>
      )}

      {!loading && loans.length > 0 && (
        <div className="loans-table-wrapper">
          <table className="loans-table">
            <thead>
              <tr>
                <th>Book</th>
                <th>Borrower</th>
                <th>Due date</th>
                <th>Status</th>
                <th aria-label="Loan actions" />
              </tr>
            </thead>
            <tbody>
              {loans.map((loan) => (
                <tr key={loan.id}>
                  <td>#{loan.bookId}</td>
                  <td>{loan.borrowerName}</td>
                  <td>{formatDate(loan.dueDate)}</td>
                  <td>
                    <span className={`loan-status ${loan.status.toLowerCase()}`}>
                      {loan.status}
                    </span>
                  </td>
                  <td className="actions">
                    {loan.status === "ACTIVE" && (
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() => onReturn(loan.id)}
                      >
                        Return
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString();
}
