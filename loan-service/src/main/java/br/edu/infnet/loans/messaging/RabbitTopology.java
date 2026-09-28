package br.edu.infnet.loans.messaging;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableRabbit
@EnableScheduling
public class RabbitTopology {

    private static final String CATALOG_ROUTING_PATTERN = "library.catalog.book.*.v1";
    private static final String DEAD_LETTER_ROUTING_KEY = "catalog-events.failed";

    @Bean
    TopicExchange libraryEventsExchange(@Value("${library.messaging.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    DirectExchange loanDeadLetterExchange(
            @Value("${library.messaging.dead-letter-exchange}") String exchangeName
    ) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    Queue catalogEventsQueue(
            @Value("${library.messaging.catalog-events-queue}") String queueName,
            @Value("${library.messaging.dead-letter-exchange}") String deadLetterExchange
    ) {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue catalogEventsDeadLetterQueue(
            @Value("${library.messaging.dead-letter-queue}") String queueName
    ) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    Binding catalogEventsBinding(Queue catalogEventsQueue, TopicExchange libraryEventsExchange) {
        return BindingBuilder.bind(catalogEventsQueue)
                .to(libraryEventsExchange)
                .with(CATALOG_ROUTING_PATTERN);
    }

    @Bean
    Binding catalogEventsDeadLetterBinding(
            Queue catalogEventsDeadLetterQueue,
            DirectExchange loanDeadLetterExchange
    ) {
        return BindingBuilder.bind(catalogEventsDeadLetterQueue)
                .to(loanDeadLetterExchange)
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    RabbitListenerContainerFactory<SimpleMessageListenerContainer> catalogEventsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup
    ) {
        Advice retryAdvice = RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(250, 2.0, 2_000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAutoStartup(autoStartup);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(retryAdvice);
        return factory;
    }
}
