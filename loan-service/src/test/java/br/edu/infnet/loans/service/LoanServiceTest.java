package br.edu.infnet.loans.service;

import br.edu.infnet.loans.dto.CreateLoanRequest;
import br.edu.infnet.loans.dto.LoanResponse;
import br.edu.infnet.loans.exception.ActiveLoanExistsException;
import br.edu.infnet.loans.exception.CatalogBookNotFoundException;
import br.edu.infnet.loans.exception.LoanAlreadyReturnedException;
import br.edu.infnet.loans.exception.LoanNotFoundException;
import br.edu.infnet.loans.messaging.LoanEventRecorder;
import br.edu.infnet.loans.model.Loan;
import br.edu.infnet.loans.model.LoanStatus;
import br.edu.infnet.loans.repository.LoanRepository;
import br.edu.infnet.loans.projection.CatalogBookProjection;
import br.edu.infnet.loans.projection.CatalogProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-19T12:00:00Z");

    @Mock
    private LoanRepository repository;

    @Mock
    private CatalogProjectionService catalogProjectionService;

    @Mock
    private LoanEventRecorder eventRecorder;

    private LoanService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new LoanService(repository, catalogProjectionService, eventRecorder, clock);
    }

    @Test
    void createsALoanForAnExistingAvailableBook() {
        CreateLoanRequest request = new CreateLoanRequest(
                7L,
                "  Maria Silva  ",
                LocalDate.parse("2026-09-02")
        );
        CatalogBookProjection projection = mockProjection();
        when(catalogProjectionService.requireAvailableBook(7L))
                .thenReturn(projection);
        when(repository.existsByBookIdAndStatus(7L, LoanStatus.ACTIVE)).thenReturn(false);
        when(repository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            ReflectionTestUtils.setField(loan, "id", 41L);
            return loan;
        });

        LoanResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(41L);
        assertThat(response.bookId()).isEqualTo(7L);
        assertThat(response.borrowerName()).isEqualTo("Maria Silva");
        assertThat(response.borrowedAt()).isEqualTo(NOW);
        assertThat(response.dueDate()).isEqualTo(LocalDate.parse("2026-09-02"));
        assertThat(response.returnedAt()).isNull();
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        verify(eventRecorder).loanCreated(any(Loan.class));
    }

    @Test
    void rejectsALoanWhenTheCatalogBookDoesNotExist() {
        when(catalogProjectionService.requireAvailableBook(99L))
                .thenThrow(new CatalogBookNotFoundException(99L));

        assertThatThrownBy(() -> service.create(requestFor(99L)))
                .isInstanceOf(CatalogBookNotFoundException.class)
                .hasMessage("Book 99 is not available in the local catalog projection");
    }

    @Test
    void rejectsASecondActiveLoanForTheSameBook() {
        CatalogBookProjection projection = mockProjection();
        when(catalogProjectionService.requireAvailableBook(7L))
                .thenReturn(projection);
        when(repository.existsByBookIdAndStatus(7L, LoanStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.create(requestFor(7L)))
                .isInstanceOf(ActiveLoanExistsException.class)
                .hasMessage("Book 7 already has an active loan");
    }

    @Test
    void returnsAnActiveLoanAtTheCurrentTime() {
        Loan active = savedLoan(15L, LoanStatus.ACTIVE);
        when(repository.findById(15L)).thenReturn(Optional.of(active));

        LoanResponse response = service.returnLoan(15L);

        assertThat(response.status()).isEqualTo(LoanStatus.RETURNED);
        assertThat(response.returnedAt()).isEqualTo(NOW);
        verify(eventRecorder).loanReturned(active);
    }

    @Test
    void rejectsReturningAnUnknownLoan() {
        when(repository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.returnLoan(50L))
                .isInstanceOf(LoanNotFoundException.class)
                .hasMessage("Loan 50 was not found");
    }

    @Test
    void rejectsReturningALoanTwice() {
        Loan returned = savedLoan(15L, LoanStatus.RETURNED);
        when(repository.findById(15L)).thenReturn(Optional.of(returned));

        assertThatThrownBy(() -> service.returnLoan(15L))
                .isInstanceOf(LoanAlreadyReturnedException.class)
                .hasMessage("Loan 15 has already been returned");
    }

    @Test
    void listsLoansInRepositoryOrder() {
        Loan first = savedLoan(2L, LoanStatus.ACTIVE);
        Loan second = savedLoan(1L, LoanStatus.RETURNED);
        when(repository.findAllByOrderByBorrowedAtDesc()).thenReturn(List.of(first, second));

        assertThat(service.list()).extracting(LoanResponse::id).containsExactly(2L, 1L);
    }

    @Test
    void getsOneExistingLoan() {
        when(repository.findById(15L)).thenReturn(Optional.of(savedLoan(15L, LoanStatus.ACTIVE)));

        assertThat(service.get(15L).id()).isEqualTo(15L);
    }

    private static CreateLoanRequest requestFor(Long bookId) {
        return new CreateLoanRequest(bookId, "Maria Silva", LocalDate.parse("2026-09-02"));
    }

    private static CatalogBookProjection mockProjection() {
        return org.mockito.Mockito.mock(CatalogBookProjection.class);
    }

    private static Loan savedLoan(Long id, LoanStatus status) {
        Loan loan = Loan.borrowed(
                7L,
                "Maria Silva",
                Instant.parse("2026-08-01T10:00:00Z"),
                LocalDate.parse("2026-08-15")
        );
        ReflectionTestUtils.setField(loan, "id", id);
        if (status == LoanStatus.RETURNED) {
            loan.markReturned(Instant.parse("2026-08-10T10:00:00Z"));
        }
        return loan;
    }

}
