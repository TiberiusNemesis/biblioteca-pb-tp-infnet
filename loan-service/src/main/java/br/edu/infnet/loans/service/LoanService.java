package br.edu.infnet.loans.service;

import br.edu.infnet.loans.dto.CreateLoanRequest;
import br.edu.infnet.loans.dto.LoanResponse;
import br.edu.infnet.loans.exception.ActiveLoanExistsException;
import br.edu.infnet.loans.exception.LoanAlreadyReturnedException;
import br.edu.infnet.loans.exception.LoanNotFoundException;
import br.edu.infnet.loans.messaging.LoanEventRecorder;
import br.edu.infnet.loans.model.Loan;
import br.edu.infnet.loans.model.LoanStatus;
import br.edu.infnet.loans.projection.CatalogProjectionService;
import br.edu.infnet.loans.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository repository;
    private final CatalogProjectionService catalogProjectionService;
    private final LoanEventRecorder eventRecorder;
    private final Clock clock;

    public LoanService(
            LoanRepository repository,
            CatalogProjectionService catalogProjectionService,
            LoanEventRecorder eventRecorder,
            Clock clock
    ) {
        this.repository = repository;
        this.catalogProjectionService = catalogProjectionService;
        this.eventRecorder = eventRecorder;
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
        catalogProjectionService.requireAvailableBook(request.bookId());

        if (repository.existsByBookIdAndStatus(request.bookId(), LoanStatus.ACTIVE)) {
            throw new ActiveLoanExistsException(request.bookId());
        }

        Loan loan = Loan.borrowed(
                request.bookId(),
                request.borrowerName().trim(),
                clock.instant(),
                request.dueDate()
        );
        Loan saved = repository.save(loan);
        eventRecorder.loanCreated(saved);
        return LoanResponse.from(saved);
    }

    @Transactional
    public LoanResponse returnLoan(Long id) {
        Loan loan = find(id);
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new LoanAlreadyReturnedException(id);
        }

        loan.markReturned(clock.instant());
        eventRecorder.loanReturned(loan);
        return LoanResponse.from(loan);
    }

    private Loan find(Long id) {
        return repository.findById(id).orElseThrow(() -> new LoanNotFoundException(id));
    }
}
