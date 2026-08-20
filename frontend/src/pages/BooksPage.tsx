import { useCallback, useEffect, useState } from "react";
import { booksApi } from "../api/booksApi";
import { loansApi } from "../api/loansApi";
import { BookForm } from "../components/BookForm";
import { BookHistoryPanel } from "../components/BookHistoryPanel";
import { BookList } from "../components/BookList";
import { ErrorBanner } from "../components/ErrorBanner";
import { LoanPanel } from "../components/LoanPanel";
import type { Book, BookHistory, BookRequest } from "../types/Book";
import type { CreateLoanRequest, Loan } from "../types/Loan";

export function BooksPage() {
  const [books, setBooks] = useState<Book[]>([]);
  const [editing, setEditing] = useState<Book | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [historyBookId, setHistoryBookId] = useState<number | null>(null);
  const [history, setHistory] = useState<BookHistory[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [loans, setLoans] = useState<Loan[]>([]);
  const [loansLoading, setLoansLoading] = useState(false);
  const [loanBusy, setLoanBusy] = useState(false);
  const [borrowing, setBorrowing] = useState<Book | undefined>(undefined);

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

  const reloadLoans = useCallback(async () => {
    setLoansLoading(true);
    try {
      setLoans(await loansApi.list());
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoansLoading(false);
    }
  }, []);

  useEffect(() => {
    reloadLoans();
  }, [reloadLoans]);

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
    if (!confirm(`Remove book #${id}?`)) return;
    setError(null);
    try {
      await booksApi.remove(id);
      setBooks((prev) => prev.filter((b) => b.id !== id));
      if (editing?.id === id) setEditing(undefined);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const handleHistory = async (id: number) => {
    setHistoryBookId(id);
    setHistory([]);
    setHistoryLoading(true);
    setError(null);
    try {
      setHistory(await booksApi.history(id));
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleCreateLoan = async (request: CreateLoanRequest) => {
    setLoanBusy(true);
    setError(null);
    try {
      const created = await loansApi.create(request);
      setLoans((current) => [created, ...current]);
      setBorrowing(undefined);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoanBusy(false);
    }
  };

  const handleReturnLoan = async (id: number) => {
    if (!confirm(`Return loan #${id}?`)) return;
    setLoanBusy(true);
    setError(null);
    try {
      const returned = await loansApi.returnLoan(id);
      setLoans((current) =>
        current.map((loan) => (loan.id === returned.id ? returned : loan)),
      );
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoanBusy(false);
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

      <BookList
        books={books}
        onBorrow={setBorrowing}
        onHistory={handleHistory}
        onEdit={setEditing}
        onRemove={handleRemove}
      />

      {historyBookId !== null && (
        <BookHistoryPanel
          bookId={historyBookId}
          history={history}
          loading={historyLoading}
          onClose={() => setHistoryBookId(null)}
        />
      )}

      <LoanPanel
        selectedBook={borrowing}
        loans={loans}
        loading={loansLoading}
        busy={loanBusy}
        onSubmit={handleCreateLoan}
        onReturn={handleReturnLoan}
        onCancelBorrow={() => setBorrowing(undefined)}
      />
    </section>
  );
}
