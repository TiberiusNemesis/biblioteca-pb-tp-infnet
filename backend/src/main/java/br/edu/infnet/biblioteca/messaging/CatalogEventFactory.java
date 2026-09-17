package br.edu.infnet.biblioteca.messaging;

import br.edu.infnet.biblioteca.messaging.outbox.OutboxEvent;
import br.edu.infnet.biblioteca.model.Book;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class CatalogEventFactory {

    private static final int EVENT_VERSION = 1;
    private static final String SOURCE = "catalog-service";
    private final ObjectMapper objectMapper;

    public CatalogEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OutboxEvent bookCreated(Book book) {
        return fullStateEvent(book, "BookCreated", "library.catalog.book.created.v1");
    }

    public OutboxEvent bookUpdated(Book book) {
        return fullStateEvent(book, "BookUpdated", "library.catalog.book.updated.v1");
    }

    public OutboxEvent bookDeleted(Book book) {
        long deletionVersion = book.getVersion() + 1;
        return event(
                book,
                "BookDeleted",
                "library.catalog.book.deleted.v1",
                new BookDeletedPayload(book.getId(), deletionVersion)
        );
    }

    private OutboxEvent fullStateEvent(Book book, String eventType, String routingKey) {
        return event(
                book,
                eventType,
                routingKey,
                new BookStatePayload(
                        book.getId(),
                        book.getTitle(),
                        book.getAuthor(),
                        book.getIsbn(),
                        book.getPublishedYear(),
                        book.getVersion()
                )
        );
    }

    private OutboxEvent event(Book book, String eventType, String routingKey, Object body) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        EventEnvelope<Object> envelope = new EventEnvelope<>(
                eventId,
                eventType,
                EVENT_VERSION,
                occurredAt,
                SOURCE,
                body
        );
        try {
            return OutboxEvent.pending(
                    eventId,
                    "Book",
                    String.valueOf(book.getId()),
                    eventType,
                    EVENT_VERSION,
                    routingKey,
                    objectMapper.writeValueAsString(envelope),
                    occurredAt
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize catalog event", ex);
        }
    }

    private record BookStatePayload(
            Long id,
            String title,
            String author,
            String isbn,
            int publishedYear,
            Long version
    ) {
    }

    private record BookDeletedPayload(Long id, long version) {
    }
}
