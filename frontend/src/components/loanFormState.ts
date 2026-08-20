import type { CreateLoanRequest } from "../types/Loan";

export interface LoanFormState {
  bookId: number;
  borrowerName: string;
  dueDate: string;
}

export function createLoanFormState(
  bookId: number,
  loanDate = new Date(),
): LoanFormState {
  const dueDate = new Date(
    loanDate.getFullYear(),
    loanDate.getMonth(),
    loanDate.getDate() + 14,
  );
  const year = dueDate.getFullYear();
  const month = String(dueDate.getMonth() + 1).padStart(2, "0");
  const day = String(dueDate.getDate()).padStart(2, "0");

  return {
    bookId,
    borrowerName: "",
    dueDate: `${year}-${month}-${day}`,
  };
}

export function updateLoanDueDate(
  state: LoanFormState,
  dueDate: string,
): LoanFormState {
  return { ...state, dueDate };
}

export function toCreateLoanRequest(
  state: LoanFormState,
): CreateLoanRequest {
  const borrowerName = state.borrowerName.trim();
  if (!borrowerName || !state.dueDate) {
    throw new Error("Borrower name and due date are required");
  }

  return {
    bookId: state.bookId,
    borrowerName,
    dueDate: state.dueDate,
  };
}
