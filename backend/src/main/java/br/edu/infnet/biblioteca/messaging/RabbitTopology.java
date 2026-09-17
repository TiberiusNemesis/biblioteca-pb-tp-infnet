package br.edu.infnet.biblioteca.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class RabbitTopology {

    @Bean
    TopicExchange libraryEventsExchange(
            @Value("${library.messaging.exchange}") String exchangeName
    ) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Clock messagingClock() {
        return Clock.systemUTC();
    }
}
