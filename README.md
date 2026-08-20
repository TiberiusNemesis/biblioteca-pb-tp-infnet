# Library Catalog and Loans - InfNet TP3

**Author:** Tiberius Dourado<br>
**Stack:** Java 21, Spring Boot 3.5, Spring Cloud OpenFeign, Spring Data JPA, PostgreSQL 16, Flyway, React 18, TypeScript, and Vite

This project manages a library catalog and its loans through two independently persisted Spring Boot services and one React interface. The catalog service owns book metadata and change history. The loan service owns borrowing and return records and validates books through the catalog REST API.

## Architecture

| Component | Responsibility | Port |
| --- | --- | --- |
| Catalog service | Books and immutable book history | `8080` |
| Loan service | Loans, returns, and book availability | `8081` |
| React frontend | Catalog and loan interface | `5173` |
| Catalog PostgreSQL | Catalog-owned tables | `5431` |
| Loan PostgreSQL | Loan-owned tables | `5433` |

The services do not share tables or database credentials. The loan service uses a Spring Cloud OpenFeign client to call `GET /api/books/{id}` before accepting a new loan.

Loan creation follows this sequence:

1. The frontend sends a loan request to the loan service.
2. Bean Validation checks the book ID, borrower name, and due date.
3. The OpenFeign client confirms that the book exists in the catalog.
4. The loan repository verifies that the book has no active loan.
5. The loan service persists and returns the new loan.

The catalog service remains available independently if the loan service is stopped. The loan service returns `503 Service Unavailable` when catalog validation cannot be completed.

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer
- Node.js 20 or newer
- Docker with Docker Compose

## Start the Application

### 1. Start Both Databases

```bash
docker compose up -d
docker compose ps
```

Docker Compose exposes the catalog database on `5431` and the loan database on `5433`. Both use named volumes, so data survives container recreation. To intentionally remove both databases, run `docker compose down -v`.

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
| `CATALOG_URL` | `http://localhost:8080` |

### 4. Start the Frontend

In a third terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. `VITE_API_URL` defaults to `http://localhost:8080/api`, and `VITE_LOAN_API_URL` defaults to `http://localhost:8081/api`.

## Domain Model

### Catalog Domain

The catalog stores the current state of each book and immutable snapshots for `CREATED`, `UPDATED`, and `DELETED` operations. ISBN values are unique, and optimistic locking protects concurrent updates.

### Loan Domain

A loan contains:

- Generated identifier
- Catalog book identifier
- Borrower name
- UTC borrowing timestamp
- Due date
- Optional UTC return timestamp
- Status: `ACTIVE` or `RETURNED`

Only one active loan may exist for a book. A PostgreSQL partial unique index reinforces this rule at the persistence boundary. Returning a loan changes its status and records the return timestamp in one transaction.

## Catalog REST API

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/books` | List current books |
| `GET` | `/api/books/{id}` | Get one current book |
| `POST` | `/api/books` | Create a book and history snapshot |
| `PUT` | `/api/books/{id}` | Update a book and add a history snapshot |
| `DELETE` | `/api/books/{id}` | Store a final snapshot and remove the book |
| `GET` | `/api/books/{id}/history` | List immutable snapshots newest first |

Book request:

```json
{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "978-0-13-235088-4",
  "publishedYear": 2008
}
```

## Loan REST API

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/loans` | List loans newest first |
| `GET` | `/api/loans/{id}` | Get one loan |
| `POST` | `/api/loans` | Borrow an existing, available book |
| `PATCH` | `/api/loans/{id}/return` | Return an active loan |

Create-loan request:

```json
{
  "bookId": 1,
  "borrowerName": "Maria Silva",
  "dueDate": "2026-09-02"
}
```

Loan response:

```json
{
  "id": 1,
  "bookId": 1,
  "borrowerName": "Maria Silva",
  "borrowedAt": "2026-08-19T12:00:00Z",
  "dueDate": "2026-09-02",
  "returnedAt": null,
  "status": "ACTIVE"
}
```

## Error Handling

Both services return structured error bodies with a timestamp, stable error code, and message. Validation errors also contain field details.

The loan service uses these main responses:

- `400 Bad Request` for invalid input
- `404 Not Found` for missing loans or catalog books
- `409 Conflict` for an active loan on the same book or a repeated return
- `503 Service Unavailable` when the catalog service cannot be reached

## Persistence

Flyway owns the production schemas. Hibernate runs with `ddl-auto: validate`, so it verifies entity mappings without altering PostgreSQL tables.

- `backend/src/main/resources/db/migration` contains catalog migrations.
- `loan-service/src/main/resources/db/migration` contains the independent loan migration.
- Catalog and loan repositories extend `JpaRepository` and remain scoped to their respective services.

## Automated Verification

Catalog tests:

```bash
mvn -f backend/pom.xml clean test
```

Loan-service tests:

```bash
mvn -f loan-service/pom.xml clean test
```

Frontend tests and production build:

```bash
npm --prefix frontend test
npm --prefix frontend run build
```

Compose validation:

```bash
docker compose config --quiet
```

The test suites cover repositories, catalog communication, loan business rules, validation, REST error mappings, catalog CRUD/history behavior, and frontend form state.

## Demonstration Scenario

1. Start both databases, both backend services, and the frontend.
2. Open the frontend and confirm the seeded catalog is displayed.
3. Select **Borrow** for a book, enter a borrower, choose a future date, and confirm.
4. Confirm the loan appears with `ACTIVE` status.
5. Attempt to borrow the same book again and observe the conflict response.
6. Return the loan and confirm its status changes to `RETURNED`.
7. Borrow the same book again to demonstrate that returned loans do not block new circulation.
8. Stop the catalog service and attempt a new loan to demonstrate the `503` integration response.

## Project Structure

```text
.
├── backend
│   └── src
│       ├── main
│       └── test
├── loan-service
│   └── src
│       ├── main
│       └── test
├── frontend
│   ├── src
│   └── test
├── docker-compose.yml
└── README.md
```
