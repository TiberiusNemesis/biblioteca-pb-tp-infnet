package br.edu.infnet.loans.messaging;

import br.edu.infnet.loans.projection.CatalogProjectionService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CatalogEventConsumer {

    private final CatalogProjectionService projectionService;

    public CatalogEventConsumer(CatalogProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @RabbitListener(
            queues = "${library.messaging.catalog-events-queue}",
            containerFactory = "catalogEventsListenerContainerFactory"
    )
    public void consume(Message message) {
        projectionService.process(new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
