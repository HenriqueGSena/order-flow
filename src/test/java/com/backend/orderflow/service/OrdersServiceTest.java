package com.backend.orderflow.service;

import com.backend.orderflow.domain.Order;
import com.backend.orderflow.domain.enuns.OrderStatus;
import com.backend.orderflow.dto.request.AppLoggerSuccessEvent;
import com.backend.orderflow.dto.request.UpdateOrdersStatusRequest;
import com.backend.orderflow.dto.response.OrderStatusAcceptedResponse;
import com.backend.orderflow.repository.OrdersRepository;
import com.backend.orderflow.repository.ProductRepository;
import com.backend.orderflow.utils.Logger.AppLogger;
import com.backend.orderflow.utils.Rabbitmq.messaging.message.OrderStatusMessage;
import com.backend.orderflow.utils.Rabbitmq.messaging.producer.OrderStatusProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdersServiceTest {

    @Mock
    private OrdersRepository ordersRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private AppLogger appLogger;

    @Mock
    private OrderStatusProducer orderStatusProducer;

    @InjectMocks
    private OrdersService ordersService;

    @Nested
    @DisplayName("updateOrderStatus() - Asynchronous Request Acceptance")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Should validate request, publish message to RabbitMQ, and return accepted response without updating order directly")
        void shouldPublishMessageAndReturnAcceptedResponseWithoutDirectMutation() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order order = Order.builder()
                    .id(orderId)
                    .orderStatus(OrderStatus.PENDING)
                    .orderDate(LocalDateTime.now())
                    .items(new ArrayList<>())
                    .build();

            UpdateOrdersStatusRequest request = new UpdateOrdersStatusRequest(OrderStatus.PROCESSING);

            when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act
            OrderStatusAcceptedResponse response = ordersService.updateOrderStatus(orderId, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.requestedStatus()).isEqualTo(OrderStatus.PROCESSING);
            assertThat(response.eventId()).isNotNull();
            assertThat(response.message()).isEqualTo("Solicitação de alteração de status aceita para processamento");

            // Verify order status was NOT modified directly in updateOrderStatus
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);

            // Verify message was published to RabbitMQ
            ArgumentCaptor<OrderStatusMessage> messageCaptor = ArgumentCaptor.forClass(OrderStatusMessage.class);
            verify(orderStatusProducer).publishStatusChange(messageCaptor.capture());

            OrderStatusMessage publishedMessage = messageCaptor.getValue();
            assertThat(publishedMessage).isNotNull();
            assertThat(publishedMessage.orderId()).isEqualTo(orderId);
            assertThat(publishedMessage.newStatus()).isEqualTo(OrderStatus.PROCESSING);
            assertThat(publishedMessage.eventId()).isEqualTo(response.eventId());

            // Verify success log event was NOT published before consumer processing
            verify(applicationEventPublisher, never()).publishEvent(any(AppLoggerSuccessEvent.class));
        }

        @Test
        @DisplayName("Should throw ResponseStatusException NOT_FOUND and not publish message when order is not found")
        void shouldThrowExceptionAndNotPublishMessageWhenOrderNotFound() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            UpdateOrdersStatusRequest request = new UpdateOrdersStatusRequest(OrderStatus.PROCESSING);

            when(ordersRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> ordersService.updateOrderStatus(orderId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verifyNoInteractions(orderStatusProducer);
            verify(applicationEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw ResponseStatusException CONFLICT and not publish message when transition is invalid")
        void shouldThrowExceptionAndNotPublishMessageWhenTransitionIsInvalid() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order order = Order.builder()
                    .id(orderId)
                    .orderStatus(OrderStatus.COMPLETED)
                    .orderDate(LocalDateTime.now())
                    .items(new ArrayList<>())
                    .build();

            UpdateOrdersStatusRequest request = new UpdateOrdersStatusRequest(OrderStatus.PROCESSING);

            when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> ordersService.updateOrderStatus(orderId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    });

            verifyNoInteractions(orderStatusProducer);
            verify(applicationEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw ResponseStatusException CONFLICT and not publish message when status is identical")
        void shouldThrowExceptionAndNotPublishMessageWhenStatusIsIdentical() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order order = Order.builder()
                    .id(orderId)
                    .orderStatus(OrderStatus.PENDING)
                    .orderDate(LocalDateTime.now())
                    .items(new ArrayList<>())
                    .build();

            UpdateOrdersStatusRequest request = new UpdateOrdersStatusRequest(OrderStatus.PENDING);

            when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> ordersService.updateOrderStatus(orderId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    });

            verifyNoInteractions(orderStatusProducer);
            verify(applicationEventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("processOrderStatus() - Consumer Async Processing")
    class ProcessOrderStatusTests {

        @Test
        @DisplayName("Should process message, update order status via dirty checking, and publish success log event")
        void shouldProcessMessageAndUpdateOrderStatusSuccessfully() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order order = Order.builder()
                    .id(orderId)
                    .orderStatus(OrderStatus.PENDING)
                    .orderDate(LocalDateTime.now())
                    .items(new ArrayList<>())
                    .build();

            OrderStatusMessage message = OrderStatusMessage.create(orderId, OrderStatus.PROCESSING);

            when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act
            ordersService.processOrderStatus(message);

            // Assert
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
            verify(applicationEventPublisher).publishEvent(any(AppLoggerSuccessEvent.class));
            verify(ordersRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle duplicate message idempotently when order already has requested status")
        void shouldHandleDuplicateMessageIdempotently() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order order = Order.builder()
                    .id(orderId)
                    .orderStatus(OrderStatus.PROCESSING)
                    .orderDate(LocalDateTime.now())
                    .items(new ArrayList<>())
                    .build();

            OrderStatusMessage message = OrderStatusMessage.create(orderId, OrderStatus.PROCESSING);

            when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act
            ordersService.processOrderStatus(message);

            // Assert - no second change, no event published
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
            verify(applicationEventPublisher, never()).publishEvent(any());
            verify(ordersRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResponseStatusException NOT_FOUND when order is not found during consumer processing")
        void shouldThrowExceptionWhenOrderNotFoundDuringProcessing() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            OrderStatusMessage message = OrderStatusMessage.create(orderId, OrderStatus.PROCESSING);

            when(ordersRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> ordersService.processOrderStatus(message))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verify(applicationEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw ResponseStatusException CONFLICT when status transition is invalid during consumer processing")
        void shouldThrowExceptionWhenTransitionIsInvalidDuringProcessing() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order order = Order.builder()
                    .id(orderId)
                    .orderStatus(OrderStatus.CANCELLED)
                    .orderDate(LocalDateTime.now())
                    .items(new ArrayList<>())
                    .build();

            OrderStatusMessage message = OrderStatusMessage.create(orderId, OrderStatus.PROCESSING);

            when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> ordersService.processOrderStatus(message))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    });

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(applicationEventPublisher, never()).publishEvent(any());
        }
    }
}
