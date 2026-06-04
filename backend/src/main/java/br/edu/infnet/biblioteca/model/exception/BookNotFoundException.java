package br.edu.infnet.biblioteca.model.exception;

public class BookNotFoundException extends RuntimeException {

    private final Long id;

    public BookNotFoundException(Long id) {
        super("Book not found: " + id);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
