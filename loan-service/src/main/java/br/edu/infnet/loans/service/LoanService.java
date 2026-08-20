package br.edu.infnet.loans.service;

import br.edu.infnet.loans.client.CatalogClient;
import br.edu.infnet.loans.dto.CreateLoanRequest;
import br.edu.infnet.loans.dto.LoanResponse;
import br.edu.infnet.loans.exception.ActiveLoanExistsException;
import br.edu.infnet.loans.exception.CatalogBookNotFoundException;
import br.edu.infnet.loans.exception.CatalogUnavailableException;
import br.edu.infnet.loans.exception.LoanAlreadyReturnedException;
import br.edu.infnet.loans.exception.LoanNotFoundException;
import br.edu.infnet.loans.model.Loan;
import br.edu.infnet.loans.model.LoanStatus;
import br.edu.infnet.loans.repository.LoanRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository repository;
    private final CatalogClient catalogClient;
    private final Clock clock;

    public LoanService(LoanRepository repository, CatalogClient catalogClient, Clock clock) {
        this.repository = repository;
        this.catalogClient = catalogClient;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> list() {
        return repository.findAllByOrderByBorrowedAtDesc().stream()
                .map(LoanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanResponse get(Long id) {
        return LoanResponse.from(find(id));
    }

    @Transactional
    public LoanResponse create(CreateLoanRequest request) {
        verifyCatalogBook(request.bookId());

        if (repository.existsByBookIdAndStatus(request.bookId(), LoanStatus.ACTIVE)) {
            throw new ActiveLoanExistsException(request.bookId());
        }

        Loan loan = Loan.borrowed(
                request.bookId(),
                request.borrowerName().trim(),
                clock.instant(),
                request.dueDate()
        );
        return LoanResponse.from(repository.save(loan));
    }

    @Transactional
    public LoanResponse returnLoan(Long id) {
        Loan loan = find(id);
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new LoanAlreadyReturnedException(id);
        }

        loan.markReturned(clock.instant());
        return LoanResponse.from(loan);
    }

    private Loan find(Long id) {
        return repository.findById(id).orElseThrow(() -> new LoanNotFoundException(id));
    }

    private void verifyCatalogBook(Long bookId) {
        try {
            catalogClient.getBook(bookId);
        } catch (FeignException.NotFound ex) {
            throw new CatalogBookNotFoundException(bookId);
        } catch (FeignException ex) {
            throw new CatalogUnavailableException();
        }
    }
}
