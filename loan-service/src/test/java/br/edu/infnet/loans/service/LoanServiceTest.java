package br.edu.infnet.loans.service;

import br.edu.infnet.loans.client.CatalogBookResponse;
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
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-19T12:00:00Z");

    @Mock
    private LoanRepository repository;

    @Mock
    private CatalogClient catalogClient;

    private LoanService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new LoanService(repository, catalogClient, clock);
    }

    @Test
    void createsALoanForAnExistingAvailableBook() {
        CreateLoanRequest request = new CreateLoanRequest(
                7L,
                "  Maria Silva  ",
                LocalDate.parse("2026-09-02")
        );
        when(catalogClient.getBook(7L)).thenReturn(catalogBook(7L));
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
    }

    @Test
    void rejectsALoanWhenTheCatalogBookDoesNotExist() {
        when(catalogClient.getBook(99L)).thenThrow(feignFailure(404));

        assertThatThrownBy(() -> service.create(requestFor(99L)))
                .isInstanceOf(CatalogBookNotFoundException.class)
                .hasMessage("Book 99 was not found in the catalog");
    }

    @Test
    void reportsCatalogOutagesWithoutCreatingALoan() {
        when(catalogClient.getBook(7L)).thenThrow(feignFailure(503));

        assertThatThrownBy(() -> service.create(requestFor(7L)))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasMessage("Catalog service is unavailable");
    }

    @Test
    void rejectsASecondActiveLoanForTheSameBook() {
        when(catalogClient.getBook(7L)).thenReturn(catalogBook(7L));
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

    private static CatalogBookResponse catalogBook(Long id) {
        return new CatalogBookResponse(
                id,
                "Clean Code",
                "Robert C. Martin",
                "978-0-13-235088-4",
                2008,
                0L,
                Instant.parse("2026-08-01T10:00:00Z"),
                Instant.parse("2026-08-01T10:00:00Z")
        );
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

    private static FeignException feignFailure(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/books/7",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        Response response = Response.builder()
                .status(status)
                .reason("Catalog response")
                .request(request)
                .headers(Map.of())
                .build();
        return FeignException.errorStatus("CatalogClient#getBook", response);
    }
}
