package br.edu.infnet.loans.messaging;

import br.edu.infnet.loans.projection.CatalogProjectionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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
    public void consume(String eventJson) {
        projectionService.process(eventJson);
    }
}
