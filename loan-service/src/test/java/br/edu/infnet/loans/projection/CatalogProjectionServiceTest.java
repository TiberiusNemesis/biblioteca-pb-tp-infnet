package br.edu.infnet.loans.projection;

import br.edu.infnet.loans.exception.CatalogBookNotFoundException;
import br.edu.infnet.loans.messaging.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CatalogProjectionServiceTest {

    @Autowired
    private CatalogProjectionService service;

    @Autowired
    private CatalogBookProjectionRepository projectionRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void createsAnAvailableProjectionFromBookCreated() {
        UUID eventId = UUID.fromString("40000000-0000-0000-0000-000000000001");

        service.process(bookEvent(eventId, "BookCreated", 7L, "Clean Code", 0L));

        CatalogBookProjection projection = service.requireAvailableBook(7L);
        assertThat(projection.getTitle()).isEqualTo("Clean Code");
        assertThat(projection.getIsbn()).isEqualTo("978-0-13-235088-4");
        assertThat(projection.getVersion()).isZero();
        assertThat(projection.isDeleted()).isFalse();
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    void appliesNewerUpdatesAndIgnoresOlderEvents() {
        service.process(bookEvent(
                UUID.fromString("40000000-0000-0000-0000-000000000002"),
                "BookUpdated",
                7L,
                "Clean Code, Third Edition",
                3L
        ));
        service.process(bookEvent(
                UUID.fromString("40000000-0000-0000-0000-000000000003"),
                "BookUpdated",
                7L,
                "Stale title",
                2L
        ));

        CatalogBookProjection projection = projectionRepository.findById(7L).orElseThrow();
        assertThat(projection.getTitle()).isEqualTo("Clean Code, Third Edition");
        assertThat(projection.getVersion()).isEqualTo(3L);
        assertThat(processedEventRepository.count()).isEqualTo(2L);
    }

    @Test
    void ignoresDuplicateEventIdentifiers() {
        UUID eventId = UUID.fromString("40000000-0000-0000-0000-000000000004");
        service.process(bookEvent(eventId, "BookCreated", 7L, "Clean Code", 0L));

        service.process(bookEvent(eventId, "BookUpdated", 7L, "Duplicate mutation", 9L));

        CatalogBookProjection projection = projectionRepository.findById(7L).orElseThrow();
        assertThat(projection.getTitle()).isEqualTo("Clean Code");
        assertThat(projection.getVersion()).isZero();
        assertThat(processedEventRepository.count()).isEqualTo(1L);
    }

    @Test
    void marksDeletedBooksUnavailableWithoutRemovingTheProjection() {
        service.process(bookEvent(
                UUID.fromString("40000000-0000-0000-0000-000000000005"),
                "BookCreated",
                7L,
                "Clean Code",
                0L
        ));
        service.process(deletedEvent(
                UUID.fromString("40000000-0000-0000-0000-000000000006"),
                7L,
                1L
        ));

        assertThat(projectionRepository.findById(7L)).get()
                .extracting(CatalogBookProjection::isDeleted)
                .isEqualTo(true);
        assertThatThrownBy(() -> service.requireAvailableBook(7L))
                .isInstanceOf(CatalogBookNotFoundException.class)
                .hasMessage("Book 7 is not available in the local catalog projection");
    }

    @Test
    void rejectsUnsupportedEventVersionsWithoutRecordingThem() {
        UUID eventId = UUID.fromString("40000000-0000-0000-0000-000000000007");
        String json = bookEvent(eventId, "BookCreated", 7L, "Clean Code", 0L)
                .replace("\"eventVersion\": 1", "\"eventVersion\": 2");

        assertThatThrownBy(() -> service.process(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported event version: 2");
        assertThat(processedEventRepository.existsById(eventId)).isFalse();
    }

    private static String bookEvent(
            UUID eventId,
            String eventType,
            Long bookId,
            String title,
            Long version
    ) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "eventVersion": 1,
                  "occurredAt": "2026-09-17T15:00:00Z",
                  "source": "catalog-service",
                  "payload": {
                    "id": %d,
                    "title": "%s",
                    "author": "Robert C. Martin",
                    "isbn": "978-0-13-235088-4",
                    "publishedYear": 2008,
                    "version": %d
                  }
                }
                """.formatted(eventId, eventType, bookId, title, version);
    }

    private static String deletedEvent(UUID eventId, Long bookId, Long version) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "BookDeleted",
                  "eventVersion": 1,
                  "occurredAt": "2026-09-17T15:00:00Z",
                  "source": "catalog-service",
                  "payload": {
                    "id": %d,
                    "version": %d
                  }
                }
                """.formatted(eventId, bookId, version);
    }
}
