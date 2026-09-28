# Event-Driven Library Catalog and Loans - InfNet TP4

**Author:** Tiberius Dourado<br>
**Stack:** Java 21, Spring Boot 3.5, Spring AMQP, RabbitMQ 4, Spring Data JPA,
PostgreSQL 16, Flyway, React 18, TypeScript, and Vite

This project manages a library catalog and its loans through two independently
persisted Spring Boot services and one React interface. The fourth delivery
replaces the synchronous service-to-service catalog lookup with versioned domain
events delivered by RabbitMQ.

The REST APIs and user interface remain synchronous at the system boundary.
Inside the system, each service commits its domain change and outgoing event in
the same database transaction. The loan service consumes catalog events into a
local projection and can therefore validate a loan without calling the catalog
service.

## Architecture

```mermaid
flowchart LR
    UI[React frontend]
    Catalog[Catalog service]
    CatalogDB[(Catalog PostgreSQL)]
    CatalogOutbox[(Catalog outbox)]
    Rabbit[(RabbitMQ<br/>library.events)]
    Loan[Loan service]
    LoanDB[(Loan PostgreSQL)]
    Projection[(Book projection)]
    LoanOutbox[(Loan outbox)]
    DLQ[(Catalog events DLQ)]

    UI -->|REST| Catalog
    UI -->|REST| Loan
    Catalog --> CatalogDB
    Catalog --> CatalogOutbox
    CatalogOutbox -->|publisher confirms| Rabbit
    Rabbit -->|book events| Loan
    Loan --> Projection
    Loan --> LoanDB
    Loan --> LoanOutbox
    LoanOutbox -->|publisher confirms| Rabbit
    Loan -. exhausted retries .-> DLQ
```

| Component | Responsibility | Port |
| --- | --- | --- |
| Catalog service | Books, immutable history, and catalog event outbox | `8080` |
| Loan service | Loans, local book projection, consumer, and loan event outbox | `8081` |
| React frontend | Catalog and loan interface | `5173` |
| Catalog PostgreSQL | Catalog-owned tables | `5431` |
| Loan PostgreSQL | Loan-owned tables and projection | `5433` |
| RabbitMQ AMQP | Event transport | `5672` |
| RabbitMQ Management | Broker administration interface | `15672` |

The services do not share tables, entities, or database credentials. OpenFeign
is no longer used. `LoanService` reads `catalog_book_projection`, which is owned
and updated locally by the loan service.

## Event Flow

```mermaid
sequenceDiagram
    actor User
    participant UI as React UI
    participant Catalog as Catalog service
    participant CDB as Catalog database
    participant Broker as RabbitMQ
    participant Consumer as Loan catalog consumer
    participant LDB as Loan database
    participant Loan as Loan service

    User->>UI: Create or update a book
    UI->>Catalog: REST request
    Catalog->>CDB: Commit book, history, and outbox event
    Catalog-->>UI: REST response
    loop Scheduled publication
        Catalog->>CDB: Read pending event
        Catalog->>Broker: Publish with confirmation
        Broker-->>Catalog: Ack
        Catalog->>CDB: Mark event as published
    end
    Broker->>Consumer: Deliver book event
    Consumer->>LDB: Commit projection and processed event ID
    User->>UI: Borrow the book
    UI->>Loan: POST /api/loans
    Loan->>LDB: Read local book projection
    Loan->>LDB: Commit loan and LoanCreated outbox event
    Loan-->>UI: Created loan
```

Delivery is at least once. Duplicate messages are safe because the loan service
stores every consumed UUID in `processed_events`. Projection versions prevent an
older event from overwriting newer book state.

## Message Contracts

All messages use the same envelope:

```json
{
  "eventId": "1adcb07c-e8d4-4ba5-a853-52b9a93fd0e8",
  "eventType": "BookCreated",
  "eventVersion": 1,
  "occurredAt": "2026-09-17T15:00:00Z",
  "source": "catalog-service",
  "payload": {}
}
```

| Event | Routing key | Payload |
| --- | --- | --- |
| `BookCreated` | `library.catalog.book.created.v1` | Complete book state |
| `BookUpdated` | `library.catalog.book.updated.v1` | Complete book state |
| `BookDeleted` | `library.catalog.book.deleted.v1` | Book ID and deletion version |
| `LoanCreated` | `library.loans.loan.created.v1` | Complete loan state |
| `LoanReturned` | `library.loans.loan.returned.v1` | Loan ID, book ID, and return time |

The durable topic exchange is `library.events`. The durable queue
`library.loan-service.catalog-events.v1` binds to
`library.catalog.book.*.v1`.

## Reliability Patterns

- **Transactional outbox:** domain mutations and outgoing messages commit in one
  PostgreSQL transaction.
- **Publisher confirms:** an outbox row is marked published only after a positive
  RabbitMQ confirmation. Catalog events additionally require a successful route
  to the projection queue. Loan-event queues remain owned by future consumers,
  so their producer requires exchange confirmation without mandatory routing.
- **Automatic recovery:** a broker failure leaves the row pending for the next
  scheduled publication cycle.
- **Idempotent consumer:** `processed_events.event_id` prevents duplicate effects.
- **Versioned projection:** stale catalog versions are recorded as processed but
  cannot overwrite current state.
- **Retry and dead letter:** catalog consumption uses three attempts with
  exponential backoff. Exhausted messages are rejected without requeue and sent
  to `library.loan-service.catalog-events.dlq` through
  `library.loan-service.dlx`.

This design improves service autonomy, failure isolation, horizontal consumer
scaling, and extensibility for future notification or analytics consumers. Its
costs are eventual consistency, more infrastructure, duplicated read data,
schema evolution discipline, and more involved operational diagnosis. It is a
good fit when services require independent availability or multiple consumers;
it is unnecessary for small applications that require immediate consistency and
have no meaningful service boundary.

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer
- Node.js 20 or newer
- Docker with Docker Compose

## Start the Application

### 1. Start PostgreSQL and RabbitMQ

```bash
docker compose up -d
docker compose ps
```

RabbitMQ Management is available at `http://localhost:15672`. The default user
and password are both `biblioteca`.

Named volumes preserve data between restarts. To intentionally delete the three
volumes and start with empty infrastructure, run `docker compose down -v`.

### 2. Start the Catalog Service

```bash
cd backend
mvn spring-boot:run
```

The catalog API is available at `http://localhost:8080/api/books`.

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5431/biblioteca` |
| `DB_USERNAME` | `biblioteca` |
| `DB_PASSWORD` | `biblioteca` |
| `RABBITMQ_HOST` | `localhost` |
| `RABBITMQ_PORT` | `5672` |
| `RABBITMQ_USERNAME` | `biblioteca` |
| `RABBITMQ_PASSWORD` | `biblioteca` |
| `OUTBOX_ENABLED` | `true` |
| `OUTBOX_FIXED_DELAY` | `1000` |
| `OUTBOX_BATCH_SIZE` | `50` |
| `OUTBOX_CONFIRM_TIMEOUT` | `5s` |

### 3. Start the Loan Service

In a second terminal:

```bash
cd loan-service
mvn spring-boot:run
```

The loan API is available at `http://localhost:8081/api/loans`.

| Variable | Default |
| --- | --- |
| `LOAN_DB_URL` | `jdbc:postgresql://localhost:5433/loans` |
| `LOAN_DB_USERNAME` | `loans` |
| `LOAN_DB_PASSWORD` | `loans` |
| `RABBITMQ_HOST` | `localhost` |
| `RABBITMQ_PORT` | `5672` |
| `RABBITMQ_USERNAME` | `biblioteca` |
| `RABBITMQ_PASSWORD` | `biblioteca` |
| `OUTBOX_ENABLED` | `true` |
| `OUTBOX_FIXED_DELAY` | `1000` |
| `OUTBOX_BATCH_SIZE` | `50` |
| `OUTBOX_CONFIRM_TIMEOUT` | `5s` |

On the first start, the catalog migration creates `BookCreated` outbox records
for existing books. Wait a few seconds for the publisher and consumer to populate
the local projection before creating the first loan.

### 4. Start the Frontend

In a third terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. `VITE_API_URL` defaults to
`http://localhost:8080/api`, and `VITE_LOAN_API_URL` defaults to
`http://localhost:8081/api`.

## REST APIs

### Catalog

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/books` | List current books |
| `GET` | `/api/books/{id}` | Get one current book |
| `POST` | `/api/books` | Create a book, history snapshot, and event |
| `PUT` | `/api/books/{id}` | Update a book, history snapshot, and event |
| `DELETE` | `/api/books/{id}` | Store final history and emit deletion event |
| `GET` | `/api/books/{id}/history` | List immutable snapshots newest first |

### Loans

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/loans` | List loans newest first |
| `GET` | `/api/loans/{id}` | Get one loan |
| `POST` | `/api/loans` | Borrow a book available in the local projection |
| `PATCH` | `/api/loans/{id}/return` | Return an active loan |

A missing, deleted, or not-yet-projected book returns `404`. This can be a brief
eventual-consistency window immediately after a book is created. An existing
active loan returns `409`.

## Automated Verification

Run from the repository root:

```bash
mvn -f backend/pom.xml clean test
mvn -f loan-service/pom.xml clean test
npm --prefix frontend test
npm --prefix frontend run build
docker compose config --quiet
```

The suites cover catalog CRUD and history, event factories, transactional
outboxes, publisher confirmations, local projection updates, duplicate and stale
messages, loan rules, retry/DLQ topology, REST validation, and frontend behavior.

## Demonstration

### Normal flow through the UI

1. Start all infrastructure, both services, and the frontend.
2. Open the frontend and create a book.
3. Wait up to two seconds, select **Borrow**, enter a borrower and a future date,
   and confirm.
4. Verify the loan appears with `ACTIVE` status.
5. Attempt another loan for the same book and observe the conflict.
6. Return the loan and verify `RETURNED` status.
7. Open RabbitMQ Management and inspect exchange `library.events` and queue
   `library.loan-service.catalog-events.v1`.

### Catalog-service outage

1. Complete the normal flow so the local projection exists.
2. Stop only the catalog Spring Boot process.
3. Call `POST http://localhost:8081/api/loans` for a projected book that has no
   active loan.
4. The loan succeeds because validation is local. Catalog mutation and catalog UI
   operations remain unavailable until that service restarts.

### Broker outage and recovery

1. Stop RabbitMQ with `docker compose stop rabbitmq`.
2. Create or update a book through the catalog REST API. The REST transaction
   still succeeds.
3. Confirm its catalog outbox row has `published_at = NULL`:

```bash
docker compose exec postgres psql -U biblioteca -d biblioteca \
  -c "SELECT event_type, attempts, published_at FROM outbox_events ORDER BY created_at DESC LIMIT 5;"
```

4. Restart the broker with `docker compose start rabbitmq`.
5. The next scheduled cycle publishes the pending event and the loan projection
   catches up automatically.

### Duplicate delivery

1. In RabbitMQ Management, open exchange `library.events`.
2. Publish the same valid `BookCreated` JSON twice with routing key
   `library.catalog.book.created.v1` and the same `eventId`.
3. Confirm only one processed marker exists:

```bash
docker compose exec loans-postgres psql -U loans -d loans \
  -c "SELECT event_id, COUNT(*) FROM processed_events GROUP BY event_id;"
```

### Dead-letter handling

1. Publish a catalog event with `eventVersion` set to `99` to exchange
   `library.events` and a matching catalog routing key.
2. The consumer rejects it after three attempts.
3. Inspect queue `library.loan-service.catalog-events.dlq` in RabbitMQ Management.
   The rejected message remains available for diagnosis and controlled replay.

## Persistence

Flyway owns the production schemas, while Hibernate uses `ddl-auto: validate`.

- Catalog: `books`, `book_history`, and `outbox_events`.
- Loan service: `loans`, `catalog_book_projection`, `processed_events`, and
  `outbox_events`.

Outbox tables retain published rows as an event audit trail. Operational cleanup
can archive old published records without touching pending ones.

## Project Structure

```text
.
├── backend
│   └── src
├── loan-service
│   └── src
├── frontend
│   └── src
├── docker-compose.yml
└── README.md
```
