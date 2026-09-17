package br.edu.infnet.loans.messaging;

import br.edu.infnet.loans.projection.CatalogProjectionService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CatalogEventConsumerTest {

    @Test
    void decodesTheAmqpBodyAsUtf8Json() {
        CatalogProjectionService projectionService = mock(CatalogProjectionService.class);
        CatalogEventConsumer consumer = new CatalogEventConsumer(projectionService);
        String json = "{\"eventType\":\"BookCreated\",\"title\":\"Ação Humana\"}";
        Message message = new Message(json.getBytes(StandardCharsets.UTF_8));

        consumer.consume(message);

        verify(projectionService).process(json);
    }
}
