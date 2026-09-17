package br.edu.infnet.loans.messaging;

import br.edu.infnet.loans.messaging.outbox.OutboxEvent;
import br.edu.infnet.loans.model.Loan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class LoanEventFactory {

    private static final int EVENT_VERSION = 1;
    private static final String SOURCE = "loan-service";
    private final ObjectMapper objectMapper;

    public LoanEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public OutboxEvent loanCreated(Loan loan) {
        return event(
                loan,
                "LoanCreated",
                "library.loans.loan.created.v1",
                new LoanCreatedPayload(
                        loan.getId(),
                        loan.getBookId(),
                        loan.getBorrowerName(),
                        loan.getBorrowedAt(),
                        loan.getDueDate(),
                        loan.getStatus().name()
                )
        );
    }

    public OutboxEvent loanReturned(Loan loan) {
        return event(
                loan,
                "LoanReturned",
                "library.loans.loan.returned.v1",
                new LoanReturnedPayload(loan.getId(), loan.getBookId(), loan.getReturnedAt())
        );
    }

    private OutboxEvent event(Loan loan, String eventType, String routingKey, Object body) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        EventEnvelope envelope = new EventEnvelope(
                eventId,
                eventType,
                EVENT_VERSION,
                occurredAt,
                SOURCE,
                objectMapper.valueToTree(body)
        );
        try {
            return OutboxEvent.pending(
                    eventId,
                    "Loan",
                    String.valueOf(loan.getId()),
                    eventType,
                    EVENT_VERSION,
                    routingKey,
                    objectMapper.writeValueAsString(envelope),
                    occurredAt
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize loan event", ex);
        }
    }

    private record LoanCreatedPayload(
            Long id,
            Long bookId,
            String borrowerName,
            Instant borrowedAt,
            LocalDate dueDate,
            String status
    ) {
    }

    private record LoanReturnedPayload(Long id, Long bookId, Instant returnedAt) {
    }
}
