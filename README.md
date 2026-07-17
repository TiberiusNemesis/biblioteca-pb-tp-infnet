# Library Catalog - InfNet TP2

**Author:** Tiberius Dourado
**Stack:** Java 21, Spring Boot 3.5, Spring Data JPA, PostgreSQL 16, Flyway, React 18, TypeScript, and Vite

This monolithic application manages a library catalog through a React interface and a Spring Boot REST API. TP2 replaces the original in-memory database with durable PostgreSQL persistence and adds immutable change history for every book mutation.

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer
- Node.js 20 or newer
- Docker with Docker Compose

## Start the Application

### 1. Start PostgreSQL

```bash
docker compose up -d
docker compose ps
```

The Compose service exposes PostgreSQL on port `5432` and stores data in the named volume `biblioteca_data`. Removing and recreating the container does not remove catalog data. To intentionally remove all local data, run `docker compose down -v`.

### 2. Start the Backend

```bash
cd backend
mvn spring-boot:run
```

The API is available at `http://localhost:8080/api/books`.

Runtime database settings can be overridden:

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/biblioteca` |
| `DB_USERNAME` | `biblioteca` |
| `DB_PASSWORD` | `biblioteca` |

Flyway applies versioned migrations before JPA starts. Hibernate uses `ddl-auto: validate`, so the application verifies mappings but never changes the production schema implicitly.

### 3. Start the Frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Set `VITE_API_URL` if the API is hosted elsewhere; its default is `http://localhost:8080/api`.

## Persistence Design

### `books`

The current catalog state contains:

- Generated primary key
- Required title, author, ISBN, and publication year
- Unique ISBN constraint
- Optimistic-lock `version`
- UTC `created_at` and `updated_at` timestamps

### `book_history`

Each successful mutation stores an immutable snapshot containing:

- Original book ID without a foreign key, so deletion history survives
- Operation: `CREATED`, `UPDATED`, or `DELETED`
- Title, author, ISBN, and publication year at that moment
- Book version and UTC change timestamp

The `(book_id, changed_at, id)` index supports deterministic newest-first history queries.

## Transaction Semantics

`BookService` owns one transaction for each use case:

1. Create or update flushes the book so its ID and version are available.
2. The service writes the corresponding history snapshot.
3. Delete writes the final snapshot before removing the current row.
4. Any exception rolls back both the catalog mutation and its history record.

Duplicate ISBN and missing-book validation occurs before history is written.

## Spring Data Repositories

The current-state repository uses derived Spring Data queries:

```java
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
}
```

History is returned newest first with a stable ID tie-breaker:

```java
public interface BookHistoryRepository extends JpaRepository<BookHistory, Long> {
    List<BookHistory> findByBookIdOrderByChangedAtDescIdDesc(Long bookId);
}
```

## REST API

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/books` | List current books |
| `GET` | `/api/books/{id}` | Get one current book |
| `POST` | `/api/books` | Create a book and a `CREATED` snapshot |
| `PUT` | `/api/books/{id}` | Update a book and add an `UPDATED` snapshot |
| `DELETE` | `/api/books/{id}` | Add a `DELETED` snapshot and remove the book |
| `GET` | `/api/books/{id}/history` | List immutable snapshots newest first |

Create or update request:

```json
{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "978-0-13-235088-4",
  "publishedYear": 2008
}
```

Book response persistence metadata:

```json
{
  "id": 1,
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "978-0-13-235088-4",
  "publishedYear": 2008,
  "version": 0,
  "createdAt": "2026-07-17T20:00:00Z",
  "updatedAt": "2026-07-17T20:00:00Z"
}
```

History response:

```json
[
  {
    "id": 3,
    "bookId": 1,
    "operation": "DELETED",
    "title": "Clean Code, Second Edition",
    "author": "Robert C. Martin",
    "isbn": "978-0-13-235088-4",
    "publishedYear": 2008,
    "bookVersion": 1,
    "changedAt": "2026-07-17T20:10:00Z"
  }
]
```

A history query for an ID with no records returns `200 OK` with an empty array.

## Database Migrations

- `V1__create_library_schema.sql` creates current-state and history tables, constraints, and indexes.
- `V2__seed_books.sql` inserts four sample catalog entries.

Never edit a migration that has already been applied. Add a new numbered migration for future schema changes.

## Automated Verification

Backend tests use isolated, test-scoped H2 in PostgreSQL compatibility mode. They cover JPA metadata, unique ISBN enforcement, optimistic version increments, history ordering, transactional service behavior, validation errors, and the full CRUD/history API flow.

```bash
cd backend
mvn clean test
```

Expected result for this submission: **18 tests, 0 failures, 0 errors**.

Frontend verification:

```bash
cd frontend
npm run build
```

Compose validation:

```bash
docker compose config --quiet
```

## Project Structure

```text
.
├── docker-compose.yml
├── backend
│   └── src
│       ├── main
│       │   ├── java/br/edu/infnet/biblioteca
│       │   │   ├── controller
│       │   │   ├── model
│       │   │   ├── repository
│       │   │   └── service
│       │   └── resources/db/migration
│       └── test
├── frontend
│   └── src
│       ├── api
│       ├── components
│       ├── pages
│       └── types
└── docs
```
