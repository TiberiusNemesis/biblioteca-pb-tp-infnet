package br.edu.infnet.biblioteca.messaging.outbox;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxPublicationService {

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;
    private final String exchange;
    private final Duration confirmTimeout;

    public OutboxPublicationService(
            OutboxEventRepository repository,
            RabbitTemplate rabbitTemplate,
            Clock clock,
            @Value("${library.messaging.exchange}") String exchange,
            @Value("${library.messaging.outbox.confirm-timeout}") Duration confirmTimeout
    ) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
        this.exchange = exchange;
        this.confirmTimeout = confirmTimeout;
    }

    @Transactional
    public void publish(UUID eventId) {
        OutboxEvent event = repository.findById(eventId).orElse(null);
        if (event == null || event.getPublishedAt() != null) {
            return;
        }

        try {
            CorrelationData correlation = new CorrelationData(eventId.toString());
            Message message = MessageBuilder
                    .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setMessageId(eventId.toString())
                    .setType(event.getEventType())
                    .setHeader("eventVersion", event.getEventVersion())
                    .build();

            rabbitTemplate.send(exchange, event.getRoutingKey(), message, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(
                    confirmTimeout.toMillis(),
                    TimeUnit.MILLISECONDS
            );
            if (!confirm.isAck()) {
                throw new IllegalStateException("Broker rejected event: " + confirm.getReason());
            }
            if (correlation.getReturned() != null) {
                throw new IllegalStateException(
                        "Event was not routed: " + correlation.getReturned().getReplyText()
                );
            }
            event.markPublished(clock.instant());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            event.markFailed("Publication interrupted");
        } catch (Exception ex) {
            event.markFailed(message(ex));
        }
    }

    private static String message(Exception ex) {
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ex.getMessage();
        }
        return ex.getClass().getSimpleName();
    }
}
