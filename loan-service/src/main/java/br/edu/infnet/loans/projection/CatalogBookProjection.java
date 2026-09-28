package br.edu.infnet.loans.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "catalog_book_projection")
public class CatalogBookProjection {

    @Id
    @Column(name = "book_id", nullable = false, updatable = false)
    private Long bookId;

    private String title;
    private String author;
    private String isbn;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(nullable = false)
    private long version;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatalogBookProjection() {
    }

    private CatalogBookProjection(Long bookId, long version, Instant updatedAt) {
        this.bookId = bookId;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public static CatalogBookProjection active(
            Long bookId,
            String title,
            String author,
            String isbn,
            Integer publishedYear,
            long version,
            Instant updatedAt
    ) {
        CatalogBookProjection projection = new CatalogBookProjection(bookId, version, updatedAt);
        projection.applyState(title, author, isbn, publishedYear, version, updatedAt);
        return projection;
    }

    public static CatalogBookProjection tombstone(Long bookId, long version, Instant updatedAt) {
        CatalogBookProjection projection = new CatalogBookProjection(bookId, version, updatedAt);
        projection.deleted = true;
        return projection;
    }

    public void applyState(
            String title,
            String author,
            String isbn,
            Integer publishedYear,
            long version,
            Instant updatedAt
    ) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
        this.version = version;
        this.deleted = false;
        this.updatedAt = updatedAt;
    }

    public void markDeleted(long version, Instant updatedAt) {
        this.version = version;
        this.deleted = true;
        this.updatedAt = updatedAt;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getIsbn() {
        return isbn;
    }

    public long getVersion() {
        return version;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
