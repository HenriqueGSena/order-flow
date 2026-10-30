package com.backend.orderflow.service;

import com.backend.orderflow.domain.Order;
import com.backend.orderflow.domain.OrderItems;
import com.backend.orderflow.domain.enuns.ProductStatus;
import com.backend.orderflow.dto.request.OrdersRequest;
import com.backend.orderflow.dto.response.OrdersResponse;
import com.backend.orderflow.mapper.OrdersMapper;
import com.backend.orderflow.repository.OrdersRepository;
import com.backend.orderflow.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
}
