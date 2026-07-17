package br.edu.infnet.biblioteca.model.dto;

import br.edu.infnet.biblioteca.model.BookHistory;
import br.edu.infnet.biblioteca.model.HistoryOperation;

import java.time.Instant;

public record BookHistoryResponse(
        Long id,
        Long bookId,
        HistoryOperation operation,
        String title,
        String author,
        String isbn,
        int publishedYear,
        Long bookVersion,
        Instant changedAt
) {
    public static BookHistoryResponse from(BookHistory history) {
        return new BookHistoryResponse(
                history.getId(),
                history.getBookId(),
                history.getOperation(),
                history.getTitle(),
                history.getAuthor(),
                history.getIsbn(),
                history.getPublishedYear(),
                history.getBookVersion(),
                history.getChangedAt()
        );
    }
}
