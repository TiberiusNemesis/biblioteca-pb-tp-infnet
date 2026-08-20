package br.edu.infnet.loans.model;

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
import java.time.LocalDate;

@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loans_borrowed_at", columnList = "borrowed_at"),
        @Index(name = "idx_loans_book_status", columnList = "book_id,status")
})
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "borrower_name", nullable = false, length = 120)
    private String borrowerName;

    @Column(name = "borrowed_at", nullable = false)
    private Instant borrowedAt;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    protected Loan() {
    }

    private Loan(Long bookId, String borrowerName, Instant borrowedAt, LocalDate dueDate) {
        this.bookId = bookId;
        this.borrowerName = borrowerName;
        this.borrowedAt = borrowedAt;
        this.dueDate = dueDate;
        this.status = LoanStatus.ACTIVE;
    }

    public static Loan borrowed(Long bookId, String borrowerName, Instant borrowedAt, LocalDate dueDate) {
        return new Loan(bookId, borrowerName, borrowedAt, dueDate);
    }

    public void markReturned(Instant returnedAt) {
        this.returnedAt = returnedAt;
        this.status = LoanStatus.RETURNED;
    }

    public Long getId() {
        return id;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public LoanStatus getStatus() {
        return status;
    }
}
