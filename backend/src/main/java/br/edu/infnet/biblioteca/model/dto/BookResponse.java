package br.edu.infnet.biblioteca.model.dto;

import br.edu.infnet.biblioteca.model.Book;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        int publishedYear
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublishedYear()
        );
    }
}
