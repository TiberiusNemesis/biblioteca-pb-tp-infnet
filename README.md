# Biblioteca — TP1 InfNet

**Autor:** Tiberius Dourado
**Stack:** Spring Boot 3.5 + Java 21 + Maven · React 18 + Vite + TypeScript · H2

Aplicação monolítica em camadas que gerencia o **acervo de uma biblioteca**, com front-end React consumindo uma API REST.

---

## Pré-requisitos

- **JDK 21+** (testado em OpenJDK 24)
- **Maven 3.9+**
- **Node.js 20+** (testado em Node 24)

## Como rodar

### 1. Back-end (porta 8080)

```bash
cd backend
mvn spring-boot:run
```

Endpoints expostos em `http://localhost:8080/api/books`.
Console do H2 disponível em `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:biblioteca`, usuário `sa`, senha vazia).

A base é populada automaticamente com 4 livros via `src/main/resources/data.sql`.

### 2. Front-end (porta 5173)

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173` no navegador.

## Testes

```bash
cd backend
mvn test
```

Resultado esperado: **11/11 testes passando** (6 unitários do `BookService` + 5 de integração via `MockMvc`).

## API REST

| Método   | Path              | Descrição             |
| -------- | ----------------- | --------------------- |
| `GET`    | `/api/books`      | Lista todos os livros |
| `GET`    | `/api/books/{id}` | Busca por id          |
| `POST`   | `/api/books`      | Cria livro            |
| `PUT`    | `/api/books/{id}` | Atualiza livro        |
| `DELETE` | `/api/books/{id}` | Remove livro          |

Schema do `BookRequest`:

```json
{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "978-0-13-235088-4",
  "publishedYear": 2008
}
```

Exemplo de erro (HTTP 409):

```json
{
  "timestamp": "2026-06-03T22:54:05.729Z",
  "error": "duplicate_isbn",
  "message": "ISBN already exists: 978-0-13-235088-4"
}
```

## Estrutura do repositório

```
.
├── backend/                  # Spring Boot
│   ├── pom.xml
│   └── src/main/java/br/edu/infnet/biblioteca/
│       ├── BibliotecaApplication.java
│       ├── controller/       # REST endpoints
│       ├── service/          # regras de negócio
│       ├── repository/       # Spring Data JPA
│       ├── model/            # entidades, DTOs, exceptions
│       └── config/           # CORS + ExceptionHandler global
└── frontend/                 # Vite + React + TS
    └── src/
        ├── api/              # cliente REST
        ├── components/       # BookList, BookForm, ErrorBanner
        ├── pages/            # BooksPage
        └── types/            # tipos espelhados do DTO
```
