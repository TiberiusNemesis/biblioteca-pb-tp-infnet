package br.edu.infnet.biblioteca.messaging.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OutboxEventRepositoryTest {

    @Autowired
    private OutboxEventRepository repository;

    @Test
    void returnsOnlyPendingEventsInCreationOrder() {
        Instant now = Instant.parse("2026-09-17T12:00:00Z");
        OutboxEvent first = repository.save(OutboxEvent.pending(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                "Book",
                "1",
                "BookCreated",
                1,
                "library.catalog.book.created.v1",
                "{\"eventId\":\"10000000-0000-0000-0000-000000000001\"}",
                now
        ));
        OutboxEvent published = repository.save(OutboxEvent.pending(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                "Book",
                "2",
                "BookCreated",
                1,
                "library.catalog.book.created.v1",
                "{}",
                now.plusSeconds(1)
        ));
        OutboxEvent last = repository.save(OutboxEvent.pending(
                UUID.fromString("10000000-0000-0000-0000-000000000003"),
                "Book",
                "3",
                "BookUpdated",
                1,
                "library.catalog.book.updated.v1",
                "{}",
                now.plusSeconds(2)
        ));
        published.markPublished(now.plusSeconds(3));
        repository.flush();

        List<OutboxEvent> pending = repository
                .findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();

        assertThat(pending).extracting(OutboxEvent::getEventId)
                .containsExactly(first.getEventId(), last.getEventId());
    }

    @Test
    void recordsFailedPublicationAttempts() {
        Instant now = Instant.parse("2026-09-17T12:00:00Z");
        OutboxEvent event = OutboxEvent.pending(
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                "Book",
                "1",
                "BookCreated",
                1,
                "library.catalog.book.created.v1",
                "{}",
                now
        );

        event.markFailed("broker unavailable");

        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("broker unavailable");
        assertThat(event.getPublishedAt()).isNull();
    }
}
