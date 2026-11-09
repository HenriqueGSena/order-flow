package com.backend.orderflow.utils.Rabbitmq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    private RabbitMQConfig rabbitMQConfig;

    @BeforeEach
    void setUp() {
        rabbitMQConfig = new RabbitMQConfig();
    }

    @Test
    void shouldCreateDurableDirectExchange() {

        // Act
        DirectExchange exchange = rabbitMQConfig.orderStatusExchange();

        // Assert
        assertThat(exchange.getName())
                .isEqualTo(RabbitMQConfig.ORDER_STATUS_EXCHANGE);

        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void shouldCreateDurableQueue() {

        // Act
        Queue queue = rabbitMQConfig.orderStatusQueue();

        // Assert
        assertThat(queue.getName())
                .isEqualTo(RabbitMQConfig.ORDER_STATUS_QUEUE);

        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.isExclusive()).isFalse();
        assertThat(queue.isAutoDelete()).isFalse();
    }

    @Test
    void shouldBindQueueToExchangeUsingCorrectRoutingKey() {

        // Act
        Binding binding = rabbitMQConfig.orderStatusBinding();

        // Assert
        assertThat(binding.getExchange())
                .isEqualTo(RabbitMQConfig.ORDER_STATUS_EXCHANGE);

        assertThat(binding.getDestination())
                .isEqualTo(RabbitMQConfig.ORDER_STATUS_QUEUE);

        assertThat(binding.getRoutingKey())
                .isEqualTo(RabbitMQConfig.ORDER_STATUS_ROUTING_KEY);
    }

    @Test
    void shouldCreateJacksonJsonMessageConverter() {

        // Act
        MessageConverter converter =
                rabbitMQConfig.jacksonMessageConverter();

        // Assert
        assertThat(converter)
                .isInstanceOf(JacksonJsonMessageConverter.class);
    }
}
