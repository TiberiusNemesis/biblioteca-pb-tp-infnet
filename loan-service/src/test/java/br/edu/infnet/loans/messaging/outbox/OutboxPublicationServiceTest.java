package br.edu.infnet.loans.messaging.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxPublicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-17T15:00:00Z");
    private OutboxEventRepository repository;
    private RabbitTemplate rabbitTemplate;
    private OutboxPublicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        service = new OutboxPublicationService(
                repository,
                rabbitTemplate,
                Clock.fixed(NOW, ZoneOffset.UTC),
                "library.events",
                Duration.ofSeconds(1)
        );
    }

    @Test
    void marksEventPublishedOnlyAfterPositiveConfirmation() {
        OutboxEvent event = event();
        when(repository.findById(event.getEventId())).thenReturn(Optional.of(event));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(
                eq("library.events"),
                eq("library.loans.loan.created.v1"),
                any(Message.class),
                any(CorrelationData.class)
        );

        service.publish(event.getEventId());

        assertThat(event.getPublishedAt()).isEqualTo(NOW);
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void keepsEventPendingWhenBrokerRejectsIt() {
        OutboxEvent event = event();
        when(repository.findById(event.getEventId())).thenReturn(Optional.of(event));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "exchange unavailable"));
            return null;
        }).when(rabbitTemplate).send(
                any(String.class),
                any(String.class),
                any(Message.class),
                any(CorrelationData.class)
        );

        service.publish(event.getEventId());

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).contains("exchange unavailable");
    }

    @Test
    void recordsPublicationExceptionWithoutLosingTheEvent() {
        OutboxEvent event = event();
        when(repository.findById(event.getEventId())).thenReturn(Optional.of(event));
        doThrow(new IllegalStateException("broker unavailable"))
                .when(rabbitTemplate).send(
                        any(String.class),
                        any(String.class),
                        any(Message.class),
                        any(CorrelationData.class)
                );

        service.publish(event.getEventId());

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("broker unavailable");
    }

    private static OutboxEvent event() {
        return OutboxEvent.pending(
                UUID.fromString("50000000-0000-0000-0000-000000000001"),
                "Loan",
                "41",
                "LoanCreated",
                1,
                "library.loans.loan.created.v1",
                "{\"eventType\":\"LoanCreated\"}",
                NOW.minusSeconds(5)
        );
    }
}
