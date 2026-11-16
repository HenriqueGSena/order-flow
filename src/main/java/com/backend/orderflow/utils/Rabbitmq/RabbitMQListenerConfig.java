package com.backend.orderflow.utils.Rabbitmq;

import com.backend.orderflow.utils.Exceptions.ordersExceptions.InvalidOrderStatusTransitionException;
import com.backend.orderflow.utils.Exceptions.ordersExceptions.OrderNotFoundException;
import com.backend.orderflow.utils.Exceptions.ordersExceptions.OrderStatusAlreadyAppliedException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.TransactionTimedOutException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.SQLTransientException;
import java.time.Duration;

/**
 * Listener retry is stateless: each delivery is retried in the listener
 * container and is rejected without requeue when recovery is exhausted.
 *
 * <p>The queue has no dead-letter exchange configured yet. Therefore,
 * {@link RejectAndDontRequeueRecoverer} prepares a definitive rejection but
 * does not create a DLQ; RabbitMQ may discard the rejected message.</p>
 */
@Configuration
public class RabbitMQListenerConfig {

    static final int ADDITIONAL_RETRIES = 3;
    static final long INITIAL_BACKOFF_MILLIS = 2_000L;
    static final double BACKOFF_MULTIPLIER = 2.0;
    static final long MAX_BACKOFF_MILLIS = 10_000L;

    @Bean
    public RetryPolicy orderStatusRetryPolicy() {
        return RetryPolicy.builder()
                .maxRetries(ADDITIONAL_RETRIES)
                .delay(Duration.ofMillis(INITIAL_BACKOFF_MILLIS))
                .multiplier(BACKOFF_MULTIPLIER)
                .maxDelay(Duration.ofMillis(MAX_BACKOFF_MILLIS))
                .predicate(RabbitMQListenerConfig::isRetryableFailure)
                .build();
    }

    @Bean
    public RetryTemplate orderStatusRetryTemplate(RetryPolicy orderStatusRetryPolicy) {
        return new RetryTemplate(orderStatusRetryPolicy);
    }

    @Bean
    public StatelessRetryOperationsInterceptor orderStatusRetryInterceptor(
            RetryPolicy orderStatusRetryPolicy) {
        return RetryInterceptorBuilder.stateless()
                .retryPolicy(orderStatusRetryPolicy)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean(name = "orderStatusRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory orderStatusRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            RetryTemplate orderStatusRetryTemplate,
            StatelessRetryOperationsInterceptor orderStatusRetryInterceptor,
            MessageConverter jacksonMessageConverter) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        factory.setRetryTemplate(orderStatusRetryTemplate);
        factory.setAdviceChain(orderStatusRetryInterceptor);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    static boolean isRetryableFailure(Throwable failure) {
        boolean retryable = false;
        Throwable current = failure;
        while (current != null) {
            if (isBusinessFailure(current)) {
                return false;
            }
            if (isTransientInfrastructureFailure(current)) {
                retryable = true;
            }
            current = current.getCause();
        }
        return retryable;
    }

    private static boolean isBusinessFailure(Throwable failure) {
        return failure instanceof InvalidOrderStatusTransitionException
                || failure instanceof OrderNotFoundException
                || failure instanceof OrderStatusAlreadyAppliedException;
    }

    private static boolean isTransientInfrastructureFailure(Throwable failure) {
        return failure instanceof TransientDataAccessException
                || failure instanceof TransactionTimedOutException
                || failure instanceof SQLTransientException
                || failure instanceof SocketTimeoutException
                || failure instanceof ConnectException;
    }

}
