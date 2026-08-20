package br.edu.infnet.loans.exception;

public class LoanAlreadyReturnedException extends RuntimeException {

    public LoanAlreadyReturnedException(Long loanId) {
        super("Loan " + loanId + " has already been returned");
    }
}
