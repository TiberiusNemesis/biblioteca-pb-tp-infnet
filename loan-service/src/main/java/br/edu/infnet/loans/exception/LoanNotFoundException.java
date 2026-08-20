package br.edu.infnet.loans.exception;

public class LoanNotFoundException extends RuntimeException {

    public LoanNotFoundException(Long loanId) {
        super("Loan " + loanId + " was not found");
    }
}
