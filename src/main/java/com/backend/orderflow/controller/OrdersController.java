package com.backend.orderflow.controller;

import com.backend.orderflow.dto.request.AddOrderItemRequest;
import com.backend.orderflow.dto.request.OrdersRequest;
import com.backend.orderflow.dto.request.UpdateOrderItemRequest;
import com.backend.orderflow.dto.request.UpdateOrdersStatusRequest;
import com.backend.orderflow.dto.response.OrderStatusAcceptedResponse;
import com.backend.orderflow.dto.response.OrdersResponse;
import com.backend.orderflow.service.OrdersService;
import com.backend.orderflow.utils.ApiResponse.ApiResponses;
import com.backend.orderflow.utils.ApiResponse.Response;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
                    HttpStatus.OK
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
                    HttpStatus.OK
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

    @PatchMapping("/updateOrderItem/{ordersId}/items/{itemId}")
    public ResponseEntity<Response<OrdersResponse>> updateOrderItem(@PathVariable UUID ordersId, @PathVariable UUID itemId, @Valid @RequestBody UpdateOrderItemRequest request) {

        try {
            var updateOrders = ordersService.updateOrderItem(ordersId, itemId, request);

            return ApiResponses.success(
                    null,
                    "Pedido atualizado com sucesso",
                    HttpStatus.OK
            );
        } catch (IllegalArgumentException exception) {
            return ApiResponses.error(exception.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ApiResponses.error(
                    "Error interno ao atualizar o pedido",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @DeleteMapping("/removeOrderItem/{ordersId}/items/{itemId}")
    public ResponseEntity<Response<OrdersResponse>> removeOrderItem(@PathVariable UUID ordersId, @PathVariable UUID itemId) {

        try {
            var deleteOrders = ordersService.removeOrderItem(ordersId, itemId);

            return ApiResponses.success(
                    null,
                    "Pedido atualizado com sucesso",
                    HttpStatus.OK
            );
        } catch (IllegalArgumentException exception) {
            return ApiResponses.error(exception.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ApiResponses.error(
                    "Error interno ao remover pedido",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/addOrderItem/{ordersId}")
    public ResponseEntity<Response<OrdersResponse>> addOrderItem(@PathVariable UUID ordersId, @Valid @RequestBody AddOrderItemRequest addOrderItemRequest) {

        try {
            var addOrders = ordersService.addOrderItem(ordersId, addOrderItemRequest);

            return ApiResponses.success(
                    null,
                    "Produto adicionado ao pedido com sucesso",
                    HttpStatus.CREATED
            );
        } catch (IllegalArgumentException exception) {
            return ApiResponses.error(exception.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ApiResponses.error(
                    "Error ao adicionar produto ao pedido",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PatchMapping("/updateOrderStatus/{ordersId}/status")
    public ResponseEntity<Response<OrderStatusAcceptedResponse>> updateOrderStatus(@PathVariable UUID ordersId, @Valid @RequestBody UpdateOrdersStatusRequest updateOrdersStatusRequest) {

        try {
            var acceptedResponse = ordersService.updateOrderStatus(ordersId, updateOrdersStatusRequest);

            return ApiResponses.success(
                    acceptedResponse,
                    "Solicitação de atualização de status aceita para processamento",
                    HttpStatus.ACCEPTED
            );
        } catch (ResponseStatusException exception) {
            return ApiResponses.error(exception.getReason(), HttpStatus.valueOf(exception.getStatusCode().value()));
        } catch (IllegalArgumentException exception) {
            return ApiResponses.error(exception.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ApiResponses.error(
                    "Error ao atualizar status do pedido",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
