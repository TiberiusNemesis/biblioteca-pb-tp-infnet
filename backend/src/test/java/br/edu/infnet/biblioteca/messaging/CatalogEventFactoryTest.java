package br.edu.infnet.biblioteca.messaging;

import br.edu.infnet.biblioteca.model.Book;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogEventFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final CatalogEventFactory factory = new CatalogEventFactory(objectMapper);

    @Test
    void createsVersionedBookCreatedEnvelopeWithCompleteState() throws Exception {
        Book book = book(7L, 0L);

        var event = factory.bookCreated(book);
        JsonNode json = objectMapper.readTree(event.getPayload());

        assertThat(event.getEventType()).isEqualTo("BookCreated");
        assertThat(event.getRoutingKey()).isEqualTo("library.catalog.book.created.v1");
        assertThat(json.path("eventVersion").asInt()).isEqualTo(1);
        assertThat(json.path("occurredAt").isTextual()).isTrue();
        assertThat(json.path("source").asText()).isEqualTo("catalog-service");
        assertThat(json.path("payload").path("id").asLong()).isEqualTo(7L);
        assertThat(json.path("payload").path("title").asText()).isEqualTo("Clean Code");
        assertThat(json.path("payload").path("isbn").asText()).isEqualTo("978-0-13-235088-4");
        assertThat(json.path("payload").path("version").asLong()).isZero();
    }

    @Test
    void advancesAggregateVersionForBookDeletion() throws Exception {
        Book book = book(7L, 3L);

        var event = factory.bookDeleted(book);
        JsonNode json = objectMapper.readTree(event.getPayload());

        assertThat(event.getEventType()).isEqualTo("BookDeleted");
        assertThat(event.getRoutingKey()).isEqualTo("library.catalog.book.deleted.v1");
        assertThat(json.path("payload").path("id").asLong()).isEqualTo(7L);
        assertThat(json.path("payload").path("version").asLong()).isEqualTo(4L);
    }

    private static Book book(Long id, Long version) {
        Book book = new Book("Clean Code", "Robert C. Martin", "978-0-13-235088-4", 2008);
        ReflectionTestUtils.setField(book, "id", id);
        ReflectionTestUtils.setField(book, "version", version);
        return book;
    }
}
