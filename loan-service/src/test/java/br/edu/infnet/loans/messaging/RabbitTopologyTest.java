package br.edu.infnet.loans.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitTopologyTest {

    private final RabbitTopology topology = new RabbitTopology();

    @Test
    void declaresDurableCatalogQueueWithDeadLetterRouting() {
        Queue queue = topology.catalogEventsQueue(
                "library.loan-service.catalog-events.v1",
                "library.loan-service.dlx"
        );

        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", "library.loan-service.dlx")
                .containsEntry("x-dead-letter-routing-key", "catalog-events.failed");
    }

    @Test
    void bindsCatalogAndDeadLetterQueuesToTheirRoutingKeys() {
        TopicExchange eventsExchange = topology.libraryEventsExchange("library.events");
        DirectExchange deadLetterExchange = topology.loanDeadLetterExchange("library.loan-service.dlx");
        Queue catalogQueue = topology.catalogEventsQueue("catalog-events", deadLetterExchange.getName());
        Queue deadLetterQueue = topology.catalogEventsDeadLetterQueue("catalog-events.dlq");

        Binding catalogBinding = topology.catalogEventsBinding(catalogQueue, eventsExchange);
        Binding deadLetterBinding = topology.catalogEventsDeadLetterBinding(
                deadLetterQueue,
                deadLetterExchange
        );

        assertThat(catalogBinding.getRoutingKey()).isEqualTo("library.catalog.book.*.v1");
        assertThat(deadLetterBinding.getRoutingKey()).isEqualTo("catalog-events.failed");
    }

    @Test
    void configuresListenerToRejectExhaustedMessagesWithoutRequeueing() {
        var factory = topology.catalogEventsListenerContainerFactory(
                mock(ConnectionFactory.class),
                false
        );

        SimpleMessageListenerContainer container = factory.createListenerContainer();

        assertThat(container.isAutoStartup()).isFalse();
        assertThat(ReflectionTestUtils.getField(container, "defaultRequeueRejected"))
                .isEqualTo(false);
        assertThat((Object[]) ReflectionTestUtils.getField(container, "adviceChain"))
                .hasSize(1);
    }
}
