package com.backend.orderflow.utils.Rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_STATUS_EXCHANGE = "order.status.exchange";

    public static final String ORDER_STATUS_QUEUE = "order.status.queue";

    public static final String ORDER_STATUS_ROUTING_KEY = "order.status.updated";

    public static final String ORDER_STATUS_DLX = "order.status.dlx";

    public static final String ORDER_STATUS_DLQ = "order.status.dlq";

    public static final String ORDER_STATUS_FAILED_ROUTING_KEY = "order.status.failed";

    @Bean
    public DirectExchange orderStatusExchange() {
        return new DirectExchange(ORDER_STATUS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderStatusQueue() {
        return QueueBuilder
                .durable(ORDER_STATUS_QUEUE)
                .deadLetterExchange(ORDER_STATUS_DLX)
                .deadLetterRoutingKey(ORDER_STATUS_FAILED_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderStatusBinding() {
        return BindingBuilder
                .bind(orderStatusQueue())
                .to(orderStatusExchange())
                .with(ORDER_STATUS_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderStatusDlx() {
        return new DirectExchange(
                ORDER_STATUS_DLX,
                true,
                false
        );
    }

    @Bean
    public Queue orderStatusDlq() {
        return QueueBuilder
                .durable(ORDER_STATUS_DLQ)
                .build();
    }

    @Bean
    public Binding orderStatusDlqBinding() {
        return BindingBuilder
                .bind(orderStatusDlq())
                .to(orderStatusDlx())
                .with(ORDER_STATUS_FAILED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
