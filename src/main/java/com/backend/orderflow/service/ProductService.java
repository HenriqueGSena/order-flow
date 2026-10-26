package com.backend.orderflow.service;

import com.backend.orderflow.domain.Product;
import com.backend.orderflow.domain.enuns.ProductStatus;
import com.backend.orderflow.dto.request.ProductRequest;
import com.backend.orderflow.dto.response.ProductResponse;
import com.backend.orderflow.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        existsProductSku(request.sku());

        var product = toEntity(request);
        var savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    private Product toEntity(ProductRequest request) {
        return Product.builder()
                .name(request.name())
                .sku(request.sku())
                .description(request.description())
                .price(request.price())
                .status(ProductStatus.ENABLED)
                .build();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getStatus(),
                product.getDescription(),
                product.getPrice(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private void existsProductSku(String sku) {
        if (productRepository.existsBySku(sku)) {
            throw new IllegalArgumentException(
                    "SKU já cadastrado: " + sku
            );
        }
    }

    private void validateDuplicateSkus(
            List<ProductRequest> requests
    ) {
        Set<String> skus = new HashSet<>();

        for (var request : requests) {
            if (!skus.add(request.sku())) {
                throw new IllegalArgumentException(
                        "SKU duplicado na lista: " + request.sku()
                );
            }
        }
    }
}

