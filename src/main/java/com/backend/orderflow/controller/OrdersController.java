package com.backend.orderflow.controller;

import com.backend.orderflow.dto.request.OrdersRequest;
import com.backend.orderflow.dto.response.OrdersResponse;
import com.backend.orderflow.service.OrdersService;
import com.backend.orderflow.utils.ApiResponse.ApiResponses;
import com.backend.orderflow.utils.ApiResponse.Response;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/getOrderById/{ordersId}")
    public ResponseEntity<Response<OrdersResponse>> getOrderById(@PathVariable UUID ordersId) {

        try {
            var orders = ordersService.getOrderById(ordersId);

            return ApiResponses.success(
                    orders,
                    "Pedido retornado com sucesso",
                    HttpStatus.CREATED
            );
        } catch (IllegalArgumentException exception) {
            return ApiResponses.error(exception.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ApiResponses.error(
                    "Error interno ao retornar o pedido",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/getListOrders")
    public ResponseEntity<Response<List<OrdersResponse>>> getListOrders() {

        try {
            var listOrders = ordersService.getListOrders();

            return ApiResponses.success(
                    listOrders,
                    "Lista de pedidos retornados com sucesso",
                    HttpStatus.CREATED
            );
        } catch (IllegalArgumentException exception) {
            return ApiResponses.error(exception.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ApiResponses.error(
                    "Error interno ao retornar a lista de pedidos",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
