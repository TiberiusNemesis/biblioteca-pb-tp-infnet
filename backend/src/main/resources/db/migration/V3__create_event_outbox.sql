CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    event_version INTEGER NOT NULL,
    routing_key VARCHAR(160) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);

CREATE INDEX idx_catalog_outbox_pending
    ON outbox_events (created_at, event_id)
    WHERE published_at IS NULL;

WITH existing_books AS (
    SELECT gen_random_uuid() AS event_id, b.*
    FROM books b
)
INSERT INTO outbox_events (
    event_id,
    aggregate_type,
    aggregate_id,
    event_type,
    event_version,
    routing_key,
    payload,
    created_at
)
SELECT
    event_id,
    'Book',
    id::text,
    'BookCreated',
    1,
    'library.catalog.book.created.v1',
    json_build_object(
        'eventId', event_id::text,
        'eventType', 'BookCreated',
        'eventVersion', 1,
        'occurredAt', to_char(
            created_at AT TIME ZONE 'UTC',
            'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'
        ),
        'source', 'catalog-service',
        'payload', json_build_object(
            'id', id,
            'title', title,
            'author', author,
            'isbn', isbn,
            'publishedYear', published_year,
            'version', version
        )
    )::text,
    created_at
FROM existing_books;
