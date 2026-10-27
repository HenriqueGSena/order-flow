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

    @Transactional
    public List<ProductResponse> createAllListProduct(List<ProductRequest> requests) {

        if (requests == null || requests.isEmpty()) throw new IllegalArgumentException("A lista de produtos não pode ser vazia");

        var skus = requests.stream()
                .map(ProductRequest::sku)
                .toList();

        validateExistingSkus(skus);
        validateDuplicateSkus(requests);

        var products = requests.stream()
                .map(this::toEntity)
                .toList();

        var saveListProducts = productRepository.saveAll(products);

        return saveListProducts.stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse findProductBySkuByProduct(String sku, String name) {
        if (sku != null && !sku.isBlank()) {
            return productRepository.findBySku(sku)
                    .map(this::toResponse)
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com SKU informado: " + sku));
        }

        if (name != null && !name.isBlank()) {
            return productRepository.findProductByName(name)
                    .map(this::toResponse)
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com o Nome informado: " + name));
        }

        throw new IllegalArgumentException("Informe o SKU ou NOME do produto");
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

    private void validateExistingSkus(List<String> skus) {

        var existingProducts = productRepository.findAllBySkuIn(skus);

        if (!existingProducts.isEmpty()) {
            var existingSkus = existingProducts.stream()
                    .map(Product::getSku)
                    .toList();

            throw new IllegalArgumentException("Os seguintes Skus já estão cadastrados: " + existingSkus);
        }
    }

    private void validateDuplicateSkus(List<ProductRequest> requests) {

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

