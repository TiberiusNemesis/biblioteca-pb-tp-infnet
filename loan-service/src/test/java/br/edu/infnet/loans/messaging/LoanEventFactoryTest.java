package br.edu.infnet.loans.messaging;

import br.edu.infnet.loans.model.Loan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LoanEventFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final LoanEventFactory factory = new LoanEventFactory(objectMapper);

    @Test
    void createsVersionedLoanCreatedEnvelopeWithCompleteState() throws Exception {
        Loan loan = activeLoan();

        var event = factory.loanCreated(loan);
        JsonNode json = objectMapper.readTree(event.getPayload());

        assertThat(event.getEventType()).isEqualTo("LoanCreated");
        assertThat(event.getRoutingKey()).isEqualTo("library.loans.loan.created.v1");
        assertThat(json.path("eventVersion").asInt()).isEqualTo(1);
        assertThat(json.path("occurredAt").isTextual()).isTrue();
        assertThat(json.path("source").asText()).isEqualTo("loan-service");
        assertThat(json.path("payload").path("id").asLong()).isEqualTo(41L);
        assertThat(json.path("payload").path("bookId").asLong()).isEqualTo(7L);
        assertThat(json.path("payload").path("borrowerName").asText()).isEqualTo("Maria Silva");
        assertThat(json.path("payload").path("returnedAt").isNull()).isTrue();
        assertThat(json.path("payload").path("status").asText()).isEqualTo("ACTIVE");
    }

    @Test
    void createsLoanReturnedEnvelopeWithReturnDetails() throws Exception {
        Loan loan = activeLoan();
        loan.markReturned(Instant.parse("2026-09-17T15:00:00Z"));

        var event = factory.loanReturned(loan);
        JsonNode json = objectMapper.readTree(event.getPayload());

        assertThat(event.getEventType()).isEqualTo("LoanReturned");
        assertThat(event.getRoutingKey()).isEqualTo("library.loans.loan.returned.v1");
        assertThat(json.path("payload").path("id").asLong()).isEqualTo(41L);
        assertThat(json.path("payload").path("bookId").asLong()).isEqualTo(7L);
        assertThat(json.path("payload").path("returnedAt").asText())
                .isEqualTo("2026-09-17T15:00:00Z");
    }

    private static Loan activeLoan() {
        Loan loan = Loan.borrowed(
                7L,
                "Maria Silva",
                Instant.parse("2026-09-01T12:00:00Z"),
                LocalDate.parse("2026-09-15")
        );
        ReflectionTestUtils.setField(loan, "id", 41L);
        return loan;
    }
}
