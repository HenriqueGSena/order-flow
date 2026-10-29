package com.backend.orderflow.service;

import com.backend.orderflow.domain.Product;
import com.backend.orderflow.domain.enuns.ProductStatus;
import com.backend.orderflow.dto.request.ProductRequest;
import com.backend.orderflow.dto.request.ProductStatusRequest;
import com.backend.orderflow.dto.response.ProductResponse;
import com.backend.orderflow.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public List<ProductResponse> getListProduct() {
        List<Product> products = productRepository.findAll();

        return products.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ProductResponse updateProduct(UUID id, ProductRequest request) {

        validUpdateProduct(request.sku());

        var updateProduct = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));

        var existsSkuByProduct = productRepository.existsBySku(request.sku());

        if (existsSkuByProduct && !updateProduct.getSku().equals(request.sku())) throw new IllegalArgumentException("SKU ja cadastrado: " + request.sku());

        updateProduct.setName(request.name());
        updateProduct.setSku(request.sku());
        updateProduct.setDescription(request.description());
        updateProduct.setPrice(request.price());

        var product = productRepository.save(updateProduct);

        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProductStatus(UUID id, ProductStatusRequest statusRequest) {
        var updateProductStatus = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));

        updateProductStatus.setStatus(statusRequest.status());
        var updateProduct = productRepository.save(updateProductStatus);

        return toResponse(updateProduct);
    }

    @Transactional
    public ProductResponse removeProduct(UUID id) {

        validateRemoverProduct(id);

        var removeProduct = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));

        productRepository.delete(removeProduct);
        return toResponse(removeProduct);
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
        if (productRepository.existsBySku(sku)) throw new IllegalArgumentException("SKU já cadastrado: " + sku);
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
            if (!skus.add(request.sku())) throw new IllegalArgumentException("SKU duplicado na lista: " + request.sku());
        }
    }

    private void validUpdateProduct(String sku) {
        if (productRepository.existsBySku(sku)) throw new IllegalArgumentException("Produto ja atualizado");
    }

    private void validateRemoverProduct(UUID id) {
        if (!productRepository.existsById(id)) throw new IllegalArgumentException("Produto inexistente ou removido da base de dados");
    }
}

