package com.backend.orderflow.utils.Rabbitmq;

import com.backend.orderflow.domain.enuns.OrderStatus;
import com.backend.orderflow.utils.Exceptions.infrastructure.MessageProcessingException;
import com.backend.orderflow.utils.Exceptions.ordersExceptions.InvalidOrderStatusTransitionException;
import com.backend.orderflow.utils.Exceptions.ordersExceptions.OrderNotFoundException;
import com.backend.orderflow.utils.Exceptions.ordersExceptions.OrderStatusAlreadyAppliedException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptor;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.util.backoff.BackOffExecution;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.SQLTransientException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQListenerConfigTest {

    private final RabbitMQListenerConfig config = new RabbitMQListenerConfig();

    @Test
    void shouldConfigureFourTotalExecutionsAndExponentialBackoff() {
        RetryPolicy policy = config.orderStatusRetryPolicy();

        assertThat(policy.shouldRetry(new TransientDataAccessResourceException("temporary"))).isTrue();
        assertThat(policy.getBackOff()).isNotNull();

        BackOffExecution backOff = policy.getBackOff().start();
        assertThat(backOff.nextBackOff()).isEqualTo(2_000L);
        assertThat(backOff.nextBackOff()).isEqualTo(4_000L);
        assertThat(backOff.nextBackOff()).isEqualTo(8_000L);
        assertThat(backOff.nextBackOff()).isEqualTo(BackOffExecution.STOP);
    }

    @Test
    void shouldRetryOnlyKnownTransientInfrastructureFailures() {
        RetryPolicy policy = config.orderStatusRetryPolicy();

        assertThat(policy.shouldRetry(new TransientDataAccessResourceException("temporary"))).isTrue();
        assertThat(policy.shouldRetry(new TransactionTimedOutException("timeout"))).isTrue();
        assertThat(policy.shouldRetry(new SQLTransientException("temporary"))).isTrue();
        assertThat(policy.shouldRetry(new SocketTimeoutException("timeout"))).isTrue();
        assertThat(policy.shouldRetry(new ConnectException("unavailable"))).isTrue();
        assertThat(policy.shouldRetry(new MessageProcessingException(
                "wrapped", new SocketTimeoutException("timeout")))).isTrue();
        assertThat(policy.shouldRetry(new IllegalStateException("unknown"))).isFalse();
    }

    @Test
    void shouldNotRetryBusinessFailuresEvenWhenWrapped() {
        RetryPolicy policy = config.orderStatusRetryPolicy();

        assertThat(policy.shouldRetry(new InvalidOrderStatusTransitionException(
                OrderStatus.COMPLETED, OrderStatus.PROCESSING))).isFalse();
        assertThat(policy.shouldRetry(new OrderNotFoundException(UUID.randomUUID()))).isFalse();
        assertThat(policy.shouldRetry(new OrderStatusAlreadyAppliedException(
                OrderStatus.PROCESSING))).isFalse();
        assertThat(policy.shouldRetry(new MessageProcessingException(
                "wrapped", new OrderNotFoundException(UUID.randomUUID())))).isFalse();
    }

    @Test
    void shouldCreateListenerFactoryWithRejectWithoutRequeueAdvice() {
        var interceptor = config.orderStatusRetryInterceptor(config.orderStatusRetryPolicy());
        SimpleRabbitListenerContainerFactory factory =
                config.orderStatusRabbitListenerContainerFactory(
                        null,
                        config.orderStatusRetryTemplate(config.orderStatusRetryPolicy()),
                        interceptor,
                        new JacksonJsonMessageConverter());

        assertThat(factory.getAdviceChain()).containsExactly(interceptor);
        assertThat(factory).isNotNull();
        assertThat(interceptor).isInstanceOf(StatelessRetryOperationsInterceptor.class);
    }

}
