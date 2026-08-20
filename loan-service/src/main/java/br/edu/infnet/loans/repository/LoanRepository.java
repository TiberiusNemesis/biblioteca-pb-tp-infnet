package br.edu.infnet.loans.repository;

import br.edu.infnet.loans.model.Loan;
import br.edu.infnet.loans.model.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    boolean existsByBookIdAndStatus(Long bookId, LoanStatus status);

    List<Loan> findAllByOrderByBorrowedAtDesc();
}
