package br.edu.infnet.loans.dto;

import br.edu.infnet.loans.model.Loan;
import br.edu.infnet.loans.model.LoanStatus;

import java.time.Instant;
import java.time.LocalDate;

public record LoanResponse(
        Long id,
        Long bookId,
        String borrowerName,
        Instant borrowedAt,
        LocalDate dueDate,
        Instant returnedAt,
        LoanStatus status
) {
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getBookId(),
                loan.getBorrowerName(),
                loan.getBorrowedAt(),
                loan.getDueDate(),
                loan.getReturnedAt(),
                loan.getStatus()
        );
    }
}
