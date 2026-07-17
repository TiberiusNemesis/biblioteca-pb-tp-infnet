package br.edu.infnet.biblioteca.model.dto;

import br.edu.infnet.biblioteca.model.Book;

import java.time.Instant;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        int publishedYear,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublishedYear(),
                book.getVersion(),
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }
}
