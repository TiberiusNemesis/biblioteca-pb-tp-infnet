package br.edu.infnet.biblioteca.model.exception;

public class DuplicateIsbnException extends RuntimeException {

    private final String isbn;

    public DuplicateIsbnException(String isbn) {
        super("ISBN already exists: " + isbn);
        this.isbn = isbn;
    }

    public String getIsbn() {
        return isbn;
    }
}
