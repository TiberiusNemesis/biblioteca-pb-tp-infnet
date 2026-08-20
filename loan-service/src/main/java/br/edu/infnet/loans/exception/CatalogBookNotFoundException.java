package br.edu.infnet.loans.exception;

public class CatalogBookNotFoundException extends RuntimeException {

    public CatalogBookNotFoundException(Long bookId) {
        super("Book " + bookId + " was not found in the catalog");
    }
}
