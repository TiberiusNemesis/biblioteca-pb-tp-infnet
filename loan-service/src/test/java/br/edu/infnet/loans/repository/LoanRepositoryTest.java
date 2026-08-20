package br.edu.infnet.loans.repository;

import br.edu.infnet.loans.model.Loan;
import br.edu.infnet.loans.model.LoanStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LoanRepositoryTest {

    @Autowired
    private LoanRepository repository;

    @Test
    void findsOnlyActiveLoansForABook() {
        Loan returned = Loan.borrowed(
                10L,
                "Alex Johnson",
                Instant.parse("2026-08-01T10:00:00Z"),
                LocalDate.parse("2026-08-15")
        );
        returned.markReturned(Instant.parse("2026-08-10T10:00:00Z"));
        repository.save(returned);

        repository.save(Loan.borrowed(
                11L,
                "Maria Silva",
                Instant.parse("2026-08-18T10:00:00Z"),
                LocalDate.parse("2026-09-01")
        ));

        assertThat(repository.existsByBookIdAndStatus(10L, LoanStatus.ACTIVE)).isFalse();
        assertThat(repository.existsByBookIdAndStatus(11L, LoanStatus.ACTIVE)).isTrue();
    }

    @Test
    void listsNewestLoansFirst() {
        Loan older = repository.save(Loan.borrowed(
                20L,
                "Older Borrower",
                Instant.parse("2026-08-01T10:00:00Z"),
                LocalDate.parse("2026-08-15")
        ));
        Loan newer = repository.save(Loan.borrowed(
                21L,
                "Newer Borrower",
                Instant.parse("2026-08-18T10:00:00Z"),
                LocalDate.parse("2026-09-01")
        ));

        assertThat(repository.findAllByOrderByBorrowedAtDesc())
                .extracting(Loan::getId)
                .containsExactly(newer.getId(), older.getId());
    }
}
