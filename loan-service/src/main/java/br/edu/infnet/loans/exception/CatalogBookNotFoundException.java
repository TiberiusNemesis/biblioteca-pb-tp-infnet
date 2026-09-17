package br.edu.infnet.loans.exception;

public class CatalogBookNotFoundException extends RuntimeException {

    public CatalogBookNotFoundException(Long bookId) {
        super("Book " + bookId + " is not available in the local catalog projection");
    }
}
