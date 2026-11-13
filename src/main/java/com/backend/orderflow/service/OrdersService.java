package com.backend.orderflow.service;

import com.backend.orderflow.domain.Order;
import com.backend.orderflow.domain.OrderItems;
import com.backend.orderflow.domain.Product;
import com.backend.orderflow.domain.enuns.OrderStatus;
import com.backend.orderflow.domain.enuns.ProductStatus;
import com.backend.orderflow.dto.request.AddOrderItemRequest;
import com.backend.orderflow.dto.request.AppLoggerRequest;
import com.backend.orderflow.dto.request.AppLoggerSuccessEvent;
import com.backend.orderflow.dto.request.OrdersRequest;
import com.backend.orderflow.dto.request.UpdateOrderItemRequest;
import com.backend.orderflow.dto.request.UpdateOrdersStatusRequest;
import com.backend.orderflow.dto.response.OrderStatusAcceptedResponse;
import com.backend.orderflow.dto.response.OrdersResponse;
import com.backend.orderflow.mapper.OrdersDetailsMapper;
import com.backend.orderflow.mapper.OrdersMapper;
import com.backend.orderflow.repository.OrdersRepository;
import com.backend.orderflow.repository.ProductRepository;
import com.backend.orderflow.utils.Logger.AppLogger;
import com.backend.orderflow.utils.Rabbitmq.messaging.message.OrderStatusMessage;
import com.backend.orderflow.utils.Rabbitmq.messaging.producer.OrderStatusProducer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class OrdersService {

    private final OrdersRepository ordersRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AppLogger appLogger;
    private final OrderStatusProducer orderStatusProducer;

    public OrdersService(OrdersRepository ordersRepository, ProductRepository productRepository, ApplicationEventPublisher applicationEventPublisher, AppLogger appLogger, OrderStatusProducer orderStatusProducer) {
        this.ordersRepository = ordersRepository;
        this.productRepository = productRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.appLogger = appLogger;
        this.orderStatusProducer = orderStatusProducer;
    }

    @Transactional
    public OrdersResponse createOrders(OrdersRequest request) {

        var order = new Order();

        for (var itemRequest : request.items()) {

            var product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> {
                        var errorMessage = "Produto não encontrado: " + itemRequest.productId();
                        var logRequest = new AppLoggerRequest(
                                "CREATE_ORDER",
                                "ORDER",
                                null,
                                "Falha ao criar pedido: Produto não encontrado",
                                appLogger.serializeSafely(request),
                                null,
                                errorMessage
                        );
                        appLogger.logError(logRequest);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage);
                    });

            if (product.getStatus() != ProductStatus.ENABLED) {
                var errorMessage = "Produto indisponível: " + product.getName();
                var logRequest = new AppLoggerRequest(
                                "CREATE_ORDER",
                                "ORDER",
                                null,
                                "Falha ao criar pedido: Produto indisponível",
                                appLogger.serializeSafely(request),
                                null,
                                errorMessage
                        );
                appLogger.logError(logRequest);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, errorMessage);
            }

            var orderItem = new OrderItems(product, itemRequest.quantity());
            order.addItem(orderItem);
        }

        var savedOrder = ordersRepository.saveAndFlush(order);

        var orderFromDatabase = ordersRepository.findById(savedOrder.getId()).orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        var response = OrdersMapper.toResponse(orderFromDatabase);

        var logRequest = new AppLoggerRequest(
                "CREATE_ORDER",
                "ORDER",
                savedOrder.getId().toString(),
                "Pedido criado com sucesso",
                appLogger.serializeSafely(request),
                appLogger.serializeSafely(response),
                null
        );

        applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));

        return response;
    }

    @Transactional(readOnly = true)
    public OrdersResponse getOrderById(UUID ordersId) {

        if (ordersId == null) {
            var errorMessage = "O ID do pedido é obrigatório";
            var logRequest = new AppLoggerRequest(
                    "GET_ORDER_BY_ID",
                    "ORDER",
                    null,
                    "Falha ao buscar pedido por ID",
                    null,
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new IllegalArgumentException(errorMessage);
        }

        var order = ordersRepository.findById(ordersId)
                .orElseThrow(() -> {
                    var errorMessage = "Pedido não encontrado com o ID informado: " + ordersId;
                    var logRequest = new AppLoggerRequest(
                            "GET_ORDER_BY_ID",
                            "ORDER",
                            ordersId.toString(),
                            "Falha ao buscar pedido por ID",
                            appLogger.serializeSafely(ordersId),
                            null,
                            errorMessage
                    );
                    appLogger.logError(logRequest);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage);
                });

        var response = OrdersMapper.toResponse(order);

        var logRequest = new AppLoggerRequest(
                "GET_ORDER_BY_ID",
                "ORDER",
                ordersId.toString(),
                "Pedido retornado com sucesso",
                appLogger.serializeSafely(ordersId),
                appLogger.serializeSafely(response),
                null
        );

        applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));

        return response;
    }

    public List<OrdersResponse> getListOrders() {

        var listOrders = ordersRepository.findAllOrderDetails();

        var response = OrdersDetailsMapper.toResponse(listOrders);

        var logRequest = new AppLoggerRequest(
                "GET_LIST_ORDERS",
                "ORDER",
                null,
                "Lista de pedidos retornada com sucesso",
                null,
                appLogger.serializeSafely(response),
                null
        );

        applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));

        return response;
    }

    @Transactional
    public OrdersResponse updateOrderItem(UUID ordersId, UUID itemId, UpdateOrderItemRequest request) {

        var order = findOrderById(ordersId, "UPDATE_ORDER_ITEM", request);

        validateOrdersStatus(order, "UPDATE_ORDER_ITEM", request);
        validateItemBelongsToOrder(order, itemId, "UPDATE_ORDER_ITEM", request);

        if (request.quantity() == null || request.quantity() <= 0) {
            var errorMessage = "A quantidade deve ser maior que zero";
            var logRequest = new AppLoggerRequest(
                    "UPDATE_ORDER_ITEM",
                    "ORDER",
                    ordersId != null ? ordersId.toString() : null,
                    "Falha ao atualizar item do pedido: Quantidade inválida",
                    appLogger.serializeSafely(request),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        var item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow();

        item.setQuantity(request.quantity());

        var response = OrdersMapper.toResponse(order);

        var logRequest = new AppLoggerRequest(
                "UPDATE_ORDER_ITEM",
                "ORDER",
                ordersId.toString(),
                "Item do pedido atualizado com sucesso",
                appLogger.serializeSafely(request),
                appLogger.serializeSafely(response),
                null
        );

        applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));

        return response;
    }

    @Transactional
    public OrdersResponse removeOrderItem(UUID ordersId, UUID itemId) {

        var order = findOrderById(ordersId, "REMOVE_ORDER_ITEM", itemId);

        validateOrdersStatus(order, "REMOVE_ORDER_ITEM", itemId);
        validateItemBelongsToOrder(order, itemId, "REMOVE_ORDER_ITEM", itemId);
        validateOrderHasItemsAfterRemoval(order, itemId, "REMOVE_ORDER_ITEM");

        order.getItems().removeIf(item -> item.getId().equals(itemId));

        var response = OrdersMapper.toResponse(order);

        var logRequest = new AppLoggerRequest(
                "REMOVE_ORDER_ITEM",
                "ORDER",
                ordersId.toString(),
                "Item removido do pedido com sucesso",
                appLogger.serializeSafely(itemId),
                appLogger.serializeSafely(response),
                null
        );

        applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));

        return response;
    }

    @Transactional
    public OrdersResponse addOrderItem(UUID ordersId, AddOrderItemRequest addOrderItemRequest) {

        var order = findOrderById(ordersId, "ADD_ORDER_ITEM", addOrderItemRequest);

        validateOrdersStatus(order, "ADD_ORDER_ITEM", addOrderItemRequest);

        var product = validateProductAvailability(addOrderItemRequest.productId(), "ADD_ORDER_ITEM", addOrderItemRequest);

        if (addOrderItemRequest.quantity() == null || addOrderItemRequest.quantity() <= 0) {
            var errorMessage = "A quantidade deve ser maior que zero";
            var logRequest = new AppLoggerRequest(
                    "ADD_ORDER_ITEM",
                    "ORDER",
                    ordersId != null ? ordersId.toString() : null,
                    "Falha ao adicionar item ao pedido: Quantidade inválida",
                    appLogger.serializeSafely(addOrderItemRequest),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        var orderItem = new OrderItems(product, addOrderItemRequest.quantity());
        order.addItem(orderItem);

        var response = OrdersMapper.toResponse(order);

        var logRequest = new AppLoggerRequest(
                "ADD_ORDER_ITEM",
                "ORDER",
                ordersId.toString(),
                "Item adicionado ao pedido com sucesso",
                appLogger.serializeSafely(addOrderItemRequest),
                appLogger.serializeSafely(response),
                null
        );

        applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));

        return response;
    }

    @Transactional(readOnly = true)
    public OrderStatusAcceptedResponse updateOrderStatus(UUID ordersId, UpdateOrdersStatusRequest updateOrdersStatusRequest) {

        var order = findOrderById(ordersId, "UPDATE_ORDER_STATUS", updateOrdersStatusRequest);

        validateOrderStatusTransition(order, updateOrdersStatusRequest.orderStatus(), updateOrdersStatusRequest);

        var message = OrderStatusMessage.create(ordersId, updateOrdersStatusRequest.orderStatus());

        orderStatusProducer.publishStatusChange(message);

        return new OrderStatusAcceptedResponse(
                message.eventId(),
                ordersId,
                updateOrdersStatusRequest.orderStatus(),
                "Solicitação de alteração de status aceita para processamento"
        );
    }

    @Transactional
    public void processOrderStatus(OrderStatusMessage message) {

        var order = findOrderById(message.orderId(), "PROCESS_ORDER_STATUS", message);

        var currentStatus = order.getOrderStatus();
        if (currentStatus == message.newStatus()) {
            return;
        }

        validateOrderStatusTransition(order, message.newStatus(), message);

        order.setOrderStatus(message.newStatus());

        var response = OrdersMapper.toResponse(order);

        var logRequest = new AppLoggerRequest(
                "UPDATE_ORDER_STATUS",
                "ORDER",
                order.getId().toString(),
                "Status do pedido atualizado de " + currentStatus + " para " + message.newStatus(),
                appLogger.serializeSafely(message),
                appLogger.serializeSafely(response),
                null
        );

        applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));
    }

    private void validateOrdersStatus(Order order, String operation, Object requestPayload) {
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            var errorMessage = "Não é possível modificar um pedido que não esteja PENDING";
            var logRequest = new AppLoggerRequest(
                    operation,
                    "ORDER",
                    order.getId() != null ? order.getId().toString() : null,
                    "Falha na operação: Pedido não está PENDING",
                    appLogger.serializeSafely(requestPayload),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }
    }

    private void validateItemBelongsToOrder(Order order, UUID itemId, String operation, Object requestPayload) {
        boolean itemExists = order.getItems().stream().anyMatch(item -> item.getId().equals(itemId));

        if (!itemExists) {
            var errorMessage = "Item não encontrado neste pedido";
            var logRequest = new AppLoggerRequest(
                    operation,
                    "ORDER",
                    order.getId() != null ? order.getId().toString() : null,
                    "Falha na operação: Item não encontrado no pedido",
                    appLogger.serializeSafely(requestPayload),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage);
        }
    }

    private void validateOrderHasItemsAfterRemoval(Order order, UUID itemId, String operation) {
        long remainingItems = order.getItems().stream()
                .filter(item -> !item.getId().equals(itemId))
                .count();

        if (remainingItems == 0) {
            var errorMessage = "O pedido precisa possuir pelo menos um item";
            var logRequest = new AppLoggerRequest(
                    operation,
                    "ORDER",
                    order.getId() != null ? order.getId().toString() : null,
                    "Falha ao remover item: Pedido deve possuir pelo menos um item",
                    appLogger.serializeSafely(itemId),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, errorMessage);
        }
    }

    private Product validateProductAvailability(UUID productId, String operation, Object requestPayload) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    var errorMessage = "Produto não encontrado: " + productId;
                    var logRequest = new AppLoggerRequest(
                            operation,
                            "ORDER",
                            null,
                            "Falha na operação: Produto não encontrado",
                            appLogger.serializeSafely(requestPayload),
                            null,
                            errorMessage
                    );
                    appLogger.logError(logRequest);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage);
                });

        if (product.getStatus() != ProductStatus.ENABLED) {
            var errorMessage = "Produto indisponível: " + product.getName();
            var logRequest = new AppLoggerRequest(
                    operation,
                    "ORDER",
                    null,
                    "Falha na operação: Produto indisponível",
                    appLogger.serializeSafely(requestPayload),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, errorMessage);
        }

        return product;
    }

    private Order findOrderById(UUID ordersId, String operation, Object requestPayload) {
        return ordersRepository.findById(ordersId).orElseThrow(() -> {
            var errorMessage = "Pedido não encontrado com ID informado: " + ordersId;
            var logRequest = new AppLoggerRequest(
                    operation,
                    "ORDER",
                    ordersId != null ? ordersId.toString() : null,
                    "Falha na operação: Pedido não encontrado",
                    appLogger.serializeSafely(requestPayload),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage);
        });
    }

    private void validateOrderStatusTransition(Order order, OrderStatus newStatus, Object requestPayload) {
        var currentStatus = order.getOrderStatus();

        if (currentStatus == newStatus) {
            var errorMessage = "O pedido já possui esse status";
            var logRequest = new AppLoggerRequest(
                    "UPDATE_ORDER_STATUS",
                    "ORDER",
                    order.getId() != null ? order.getId().toString() : null,
                    "O pedido já possui o status solicitado",
                    appLogger.serializeSafely(requestPayload),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }

        boolean validTransition = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!validTransition) {
            var errorMessage = "Transição de status não permitida: " + currentStatus + " -> " + newStatus;
            var logRequest = new AppLoggerRequest(
                    "UPDATE_ORDER_STATUS",
                    "ORDER",
                    order.getId() != null ? order.getId().toString() : null,
                    "Falha ao atualizar status do pedido: Transição não permitida",
                    appLogger.serializeSafely(requestPayload),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    errorMessage
            );
        }
    }
}
