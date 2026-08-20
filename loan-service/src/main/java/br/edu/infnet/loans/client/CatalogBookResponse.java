package br.edu.infnet.loans.client;

import java.time.Instant;

public record CatalogBookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        int publishedYear,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
