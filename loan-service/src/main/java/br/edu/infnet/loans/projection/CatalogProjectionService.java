package br.edu.infnet.loans.projection;

import br.edu.infnet.loans.exception.CatalogBookNotFoundException;
import br.edu.infnet.loans.messaging.EventEnvelope;
import br.edu.infnet.loans.messaging.ProcessedEvent;
import br.edu.infnet.loans.messaging.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CatalogProjectionService {

    private static final int SUPPORTED_EVENT_VERSION = 1;
    private static final String EXPECTED_SOURCE = "catalog-service";

    private final CatalogBookProjectionRepository projectionRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CatalogProjectionService(
            CatalogBookProjectionRepository projectionRepository,
            ProcessedEventRepository processedEventRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.projectionRepository = projectionRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void process(String eventJson) {
        EventEnvelope event = deserialize(eventJson);
        validate(event);

        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        switch (event.eventType()) {
            case "BookCreated", "BookUpdated" -> applyBookState(event.payload());
            case "BookDeleted" -> applyDeletion(event.payload());
            default -> throw new IllegalArgumentException("Unsupported event type: " + event.eventType());
        }

        processedEventRepository.save(new ProcessedEvent(
                event.eventId(),
                event.eventType(),
                clock.instant()
        ));
    }

    @Transactional(readOnly = true)
    public CatalogBookProjection requireAvailableBook(Long bookId) {
        return projectionRepository.findById(bookId)
                .filter(projection -> !projection.isDeleted())
                .orElseThrow(() -> new CatalogBookNotFoundException(bookId));
    }

    private EventEnvelope deserialize(String eventJson) {
        try {
            return objectMapper.readValue(eventJson, EventEnvelope.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid catalog event", ex);
        }
    }

    private static void validate(EventEnvelope event) {
        if (event.eventVersion() != SUPPORTED_EVENT_VERSION) {
            throw new IllegalArgumentException("Unsupported event version: " + event.eventVersion());
        }
        if (!EXPECTED_SOURCE.equals(event.source())) {
            throw new IllegalArgumentException("Unsupported event source: " + event.source());
        }
        if (event.eventId() == null || event.payload() == null) {
            throw new IllegalArgumentException("Catalog event is missing required fields");
        }
    }

    private void applyBookState(JsonNode payload) {
        Long bookId = requiredLong(payload, "id");
        long incomingVersion = requiredLong(payload, "version");
        CatalogBookProjection projection = projectionRepository.findById(bookId).orElse(null);

        if (projection != null && incomingVersion < projection.getVersion()) {
            return;
        }

        if (projection == null) {
            projection = CatalogBookProjection.active(
                    bookId,
                    requiredText(payload, "title"),
                    requiredText(payload, "author"),
                    requiredText(payload, "isbn"),
                    Math.toIntExact(requiredLong(payload, "publishedYear")),
                    incomingVersion,
                    clock.instant()
            );
        } else {
            projection.applyState(
                    requiredText(payload, "title"),
                    requiredText(payload, "author"),
                    requiredText(payload, "isbn"),
                    Math.toIntExact(requiredLong(payload, "publishedYear")),
                    incomingVersion,
                    clock.instant()
            );
        }
        projectionRepository.save(projection);
    }

    private void applyDeletion(JsonNode payload) {
        Long bookId = requiredLong(payload, "id");
        long incomingVersion = requiredLong(payload, "version");
        CatalogBookProjection projection = projectionRepository.findById(bookId).orElse(null);

        if (projection != null && incomingVersion < projection.getVersion()) {
            return;
        }

        if (projection == null) {
            projection = CatalogBookProjection.tombstone(bookId, incomingVersion, clock.instant());
        } else {
            projection.markDeleted(incomingVersion, clock.instant());
        }
        projectionRepository.save(projection);
    }

    private static long requiredLong(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        if (value == null || !value.canConvertToLong()) {
            throw new IllegalArgumentException("Catalog event payload is missing field: " + fieldName);
        }
        return value.longValue();
    }

    private static String requiredText(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("Catalog event payload is missing field: " + fieldName);
        }
        return value.textValue();
    }
}
