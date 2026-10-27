package com.backend.orderflow.controller;

import com.backend.orderflow.dto.request.ProductRequest;
import com.backend.orderflow.dto.response.ProductResponse;
import com.backend.orderflow.service.ProductService;
import com.backend.orderflow.utils.ApiResponse.ApiResponses;
import com.backend.orderflow.utils.ApiResponse.Response;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            return ApiResponses.error(exception.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ApiResponses.error(
                    "Error interno ao cadastrar um novo produto no sistema",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/createProductList")
    public ResponseEntity<Response<List<ProductResponse>>> createProductList(@RequestBody List<@Valid ProductRequest> requestList) {
        try {
            var productList = productService.createAllListProduct(requestList);
            return ApiResponses.success(
                    productList,
                    "Lista de produto criado com sucesso",
                    HttpStatus.CREATED
            );
        } catch (IllegalArgumentException exception) {
            return ApiResponses.error(exception.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ApiResponses.error(
                    "Error interno ao cadastrar um novo produto no sistema",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/searchProduct")
    public ResponseEntity<Response<ProductResponse>> findProductBySku(@RequestParam(required = false) String sku, @RequestParam(required = false) String name) {
        try {
            var productSku = productService.findProductBySkuByProduct(sku, name);
            return ApiResponses.success(
                    productSku,
                    "Produto encontrado com sucesso",
                    HttpStatus.OK);

        } catch (Exception e) {
            return ApiResponses.error(
                    "Error interno ao buscar o produto",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

}
