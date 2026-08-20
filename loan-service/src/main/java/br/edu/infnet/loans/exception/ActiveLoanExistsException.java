package br.edu.infnet.loans.exception;

public class ActiveLoanExistsException extends RuntimeException {

    public ActiveLoanExistsException(Long bookId) {
        super("Book " + bookId + " already has an active loan");
    }
}
