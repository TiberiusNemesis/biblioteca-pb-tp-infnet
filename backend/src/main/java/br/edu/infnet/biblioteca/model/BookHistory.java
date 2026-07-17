package br.edu.infnet.biblioteca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "book_history",
        indexes = @Index(
                name = "idx_book_history_book_changed",
                columnList = "book_id, changed_at, id"
        )
)
public class BookHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HistoryOperation operation;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 120)
    private String author;

    @Column(nullable = false, length = 20)
    private String isbn;

    @Column(name = "published_year", nullable = false)
    private int publishedYear;

    @Column(name = "book_version", nullable = false)
    private Long bookVersion;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected BookHistory() {
    }

    public static BookHistory snapshot(Book book, HistoryOperation operation) {
        BookHistory history = new BookHistory();
        history.bookId = book.getId();
        history.operation = operation;
        history.title = book.getTitle();
        history.author = book.getAuthor();
        history.isbn = book.getIsbn();
        history.publishedYear = book.getPublishedYear();
        history.bookVersion = book.getVersion();
        history.changedAt = Instant.now();
        return history;
    }

    public Long getId() {
        return id;
    }

    public Long getBookId() {
        return bookId;
    }

    public HistoryOperation getOperation() {
        return operation;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public int getPublishedYear() {
        return publishedYear;
    }

    public Long getBookVersion() {
        return bookVersion;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
