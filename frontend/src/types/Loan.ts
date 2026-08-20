export type LoanStatus = "ACTIVE" | "RETURNED";

export interface Loan {
  id: number;
  bookId: number;
  borrowerName: string;
  borrowedAt: string;
  dueDate: string;
  returnedAt: string | null;
  status: LoanStatus;
}

export interface CreateLoanRequest {
  bookId: number;
  borrowerName: string;
  dueDate: string;
}
