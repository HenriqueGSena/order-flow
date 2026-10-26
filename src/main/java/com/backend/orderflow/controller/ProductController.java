package com.backend.orderflow.controller;

import com.backend.orderflow.dto.request.ProductRequest;
import com.backend.orderflow.dto.response.ProductResponse;
import com.backend.orderflow.service.ProductService;
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
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/createProduct")
    public ResponseEntity<Response<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        try {
            var product = productService.createProduct(request);

            return ApiResponses.success(
                    product,
                    "Produto criado com sucesso",
                    HttpStatus.CREATED
            );
        } catch (IllegalArgumentException exception) {
            return ApiResponses.error(
                    exception.getMessage(),
                    HttpStatus.CONFLICT
            );
        } catch (Exception e) {
            return ApiResponses.error(
                    "Error interno ao cadastrar um novo produto no sistema",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

}
