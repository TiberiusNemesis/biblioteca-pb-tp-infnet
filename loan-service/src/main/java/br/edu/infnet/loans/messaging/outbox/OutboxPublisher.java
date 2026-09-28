package br.edu.infnet.loans.messaging.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(
        name = "library.messaging.outbox.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxPublisher {

    private final OutboxEventRepository repository;
    private final OutboxPublicationService publicationService;
    private final int batchSize;
    private final AtomicBoolean publishing = new AtomicBoolean();

    public OutboxPublisher(
            OutboxEventRepository repository,
            OutboxPublicationService publicationService,
            @Value("${library.messaging.outbox.batch-size:50}") int batchSize
    ) {
        this.repository = repository;
        this.publicationService = publicationService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${library.messaging.outbox.fixed-delay:1000}")
    public void publishPending() {
        if (!publishing.compareAndSet(false, true)) {
            return;
        }
        try {
            repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc().stream()
                    .limit(batchSize)
                    .map(OutboxEvent::getEventId)
                    .forEach(publicationService::publish);
        } finally {
            publishing.set(false);
        }
    }
}
