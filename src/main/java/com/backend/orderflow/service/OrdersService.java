package com.backend.orderflow.service;

import com.backend.orderflow.domain.Order;
import com.backend.orderflow.domain.OrderItems;
import com.backend.orderflow.domain.Product;
import com.backend.orderflow.domain.enuns.OrderStatus;
import com.backend.orderflow.domain.enuns.ProductStatus;
import com.backend.orderflow.dto.request.AddOrderItemRequest;
import com.backend.orderflow.dto.request.OrdersRequest;
import com.backend.orderflow.dto.request.UpdateOrderItemRequest;
import com.backend.orderflow.dto.request.UpdateOrdersStatusRequest;
import com.backend.orderflow.dto.response.OrdersResponse;
import com.backend.orderflow.mapper.OrdersDetailsMapper;
import com.backend.orderflow.mapper.OrdersMapper;
import com.backend.orderflow.repository.OrdersRepository;
import com.backend.orderflow.repository.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class OrdersService {

    private final OrdersRepository ordersRepository;

    private final ProductRepository productRepository;

    public OrdersService(
            OrdersRepository ordersRepository,
            ProductRepository productRepository
    ) {
        this.ordersRepository = ordersRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrdersResponse createOrders(OrdersRequest request) {

        var order = new Order();

        for (var itemRequest : request.items()) {

            var product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado: " + itemRequest.productId()));

            if (product.getStatus() != ProductStatus.ENABLED) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Produto indisponível: " + product.getName());

            var orderItem = new OrderItems(product, itemRequest.quantity());
            order.addItem(orderItem);
        }

        var savedOrder = ordersRepository.saveAndFlush(order);

        var orderFromDatabase = ordersRepository.findById(savedOrder.getId()).orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        return OrdersMapper.toResponse(orderFromDatabase);
    }

    @Transactional(readOnly = true)
    public OrdersResponse getOrderById(UUID ordersId) {

        if (ordersId == null) throw new IllegalArgumentException("O ID do pedido é obrigatório");

        return ordersRepository.findById(ordersId)
                .map(OrdersMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado com o ID informado: " + ordersId));
    }

    public List<OrdersResponse> getListOrders() {

        var listOrders = ordersRepository.findAllOrderDetails();

        return OrdersDetailsMapper.toResponse(listOrders);
    }

    @Transactional
    public OrdersResponse updateOrderItem(UUID ordersId, UUID itemId, UpdateOrderItemRequest request) {

        var order = findOrderById(ordersId);

        validateOrdersStatus(order);
        validateItemBelongsToOrder(order, itemId);

        if (request.quantity() == null || request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A quantidade deve ser maior que zero");
        }

        var item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow();

        item.setQuantity(request.quantity());

        return OrdersMapper.toResponse(order);
    }

    @Transactional
    public OrdersResponse removeOrderItem(UUID ordersId, UUID itemId) {

        var order = findOrderById(ordersId);

        validateOrdersStatus(order);
        validateItemBelongsToOrder(order, itemId);
        validateOrderHasItemsAfterRemoval(order, itemId);

        order.getItems().removeIf(item -> item.getId().equals(itemId));

        return OrdersMapper.toResponse(order);
    }

    @Transactional
    public OrdersResponse addOrderItem(UUID ordersId, AddOrderItemRequest addOrderItemRequest) {

        var order = findOrderById(ordersId);

        validateOrdersStatus(order);

        var product = validateProductAvailability(addOrderItemRequest.productId());

        if (addOrderItemRequest.quantity() == null || addOrderItemRequest.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A quantidade deve ser maior que zero");
        }

        var orderItem = new OrderItems(product, addOrderItemRequest.quantity());
        order.addItem(orderItem);

        return OrdersMapper.toResponse(order);
    }

    @Transactional
    public OrdersResponse updateOrderStatus(UUID ordersId, UpdateOrdersStatusRequest updateOrdersStatusRequest) {

        var ordersStatus = findOrderById(ordersId);

        validateOrderStatusTransition(ordersStatus, updateOrdersStatusRequest.orderStatus());
        ordersStatus.setOrderStatus(updateOrdersStatusRequest.orderStatus());

        return OrdersMapper.toResponse(ordersStatus);
    }

    private void validateOrdersStatus(Order order) {
        if (order.getOrderStatus() != OrderStatus.PENDING) throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível modificar um pedido que não esteja PENDING");
    }

    private void validateItemBelongsToOrder(Order order, UUID itemId) {

        boolean itemExists = order.getItems().stream().anyMatch(item -> item.getId().equals(itemId));

        if (!itemExists) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado neste pedido");
    }

    private void validateOrderHasItemsAfterRemoval(Order order, UUID itemId) {

        long remainingItems = order.getItems().stream()
                .filter(item -> !item.getId().equals(itemId))
                .count();

        if (remainingItems == 0) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "O pedido precisa possuir pelo menos um item");
    }

    private Product validateProductAvailability(UUID productId) {

        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produto não encontrado: " + productId
                ));

        if (product.getStatus() != ProductStatus.ENABLED) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Produto indisponível: " + product.getName());

        return product;
    }

    private Order findOrderById(UUID ordersId) {
        return ordersRepository.findById(ordersId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado com ID informado: " + ordersId));
    }

    private void validateOrderStatusTransition(Order order, OrderStatus newStatus) {

        var currentStatus = order.getOrderStatus();

        if (currentStatus == newStatus) throw new ResponseStatusException(HttpStatus.CONFLICT, "O pedido já possui esse status");

        boolean validTransition = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;

            case PROCESSING -> newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED;

            case COMPLETED, CANCELLED -> false;
        };

        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transição de status não permitida: "
                            + currentStatus + " -> " + newStatus
            );
        }
    }
}
