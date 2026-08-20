package br.edu.infnet.loans.exception;

public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException() {
        super("Catalog service is unavailable");
    }
}
