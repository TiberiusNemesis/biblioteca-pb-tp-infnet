package br.edu.infnet.biblioteca.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 80, updatable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 80, updatable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 80, updatable = false)
    private String eventType;

    @Column(name = "event_version", nullable = false, updatable = false)
    private int eventVersion;

    @Column(name = "routing_key", nullable = false, length = 160, updatable = false)
    private String routingKey;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected OutboxEvent() {
    }

    private OutboxEvent(
            UUID eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            int eventVersion,
            String routingKey,
            String payload,
            Instant createdAt
    ) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public static OutboxEvent pending(
            UUID eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            int eventVersion,
            String routingKey,
            String payload,
            Instant createdAt
    ) {
        return new OutboxEvent(
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                eventVersion,
                routingKey,
                payload,
                createdAt
        );
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(String error) {
        attempts++;
        lastError = error == null ? "Unknown publication failure" : error.substring(0, Math.min(error.length(), 1000));
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }
}
