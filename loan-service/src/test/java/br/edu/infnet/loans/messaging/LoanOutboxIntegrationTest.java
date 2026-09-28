package br.edu.infnet.loans.messaging;

import br.edu.infnet.loans.dto.CreateLoanRequest;
import br.edu.infnet.loans.messaging.outbox.OutboxEvent;
import br.edu.infnet.loans.messaging.outbox.OutboxEventRepository;
import br.edu.infnet.loans.projection.CatalogProjectionService;
import br.edu.infnet.loans.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class LoanOutboxIntegrationTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private CatalogProjectionService projectionService;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Test
    void recordsCreatedAndReturnedEventsWithTheLoanChanges() {
        projectionService.process(bookCreatedEvent());

        var created = loanService.create(new CreateLoanRequest(
                7L,
                "Maria Silva",
                LocalDate.parse("2100-01-15")
        ));
        loanService.returnLoan(created.id());

        assertThat(outboxRepository.findAll())
                .extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("LoanCreated", "LoanReturned");
        assertThat(outboxRepository.findAll())
                .allSatisfy(event -> assertThat(event.getPublishedAt()).isNull());
    }

    private static String bookCreatedEvent() {
        return """
                {
                  "eventId": "60000000-0000-0000-0000-000000000001",
                  "eventType": "BookCreated",
                  "eventVersion": 1,
                  "occurredAt": "2026-09-17T15:00:00Z",
                  "source": "catalog-service",
                  "payload": {
                    "id": 7,
                    "title": "Clean Code",
                    "author": "Robert C. Martin",
                    "isbn": "978-0-13-235088-4",
                    "publishedYear": 2008,
                    "version": 0
                  }
                }
                """;
    }
}
