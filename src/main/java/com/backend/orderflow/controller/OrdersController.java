package com.backend.orderflow.controller;

import com.backend.orderflow.dto.request.OrdersRequest;
import com.backend.orderflow.dto.response.OrdersResponse;
import com.backend.orderflow.service.OrdersService;
import com.backend.orderflow.utils.ApiResponse.ApiResponses;
import com.backend.orderflow.utils.ApiResponse.Response;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrdersController {

    private final OrdersService ordersService;

    public OrdersController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @PostMapping("/createOrders")
    public ResponseEntity<Response<OrdersResponse>> createOrders(@Valid @RequestBody OrdersRequest request) {
        try {
            var orders = ordersService.createOrders(request);

            return ApiResponses.success(
                    orders,
                    "Pedido criado com sucesso",
                    HttpStatus.CREATED
            );
        } catch (IllegalArgumentException exception) {
            return ApiResponses.error(exception.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ApiResponses.error(
                    "Error interno ao criar um pedido",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
