package com.backend.orderflow.service;

import com.backend.orderflow.domain.Product;
import com.backend.orderflow.domain.enuns.ProductStatus;
import com.backend.orderflow.dto.request.AppLoggerRequest;
import com.backend.orderflow.dto.request.AppLoggerSuccessEvent;
import com.backend.orderflow.dto.request.ProductRequest;
import com.backend.orderflow.dto.request.ProductStatusRequest;
import com.backend.orderflow.dto.response.ProductResponse;
import com.backend.orderflow.repository.ProductRepository;
import com.backend.orderflow.utils.Logger.AppLogger;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AppLogger appLogger;

    public ProductService(ProductRepository productRepository, ApplicationEventPublisher applicationEventPublisher, AppLogger appLogger) {
        this.productRepository = productRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.appLogger = appLogger;
    }

    public ProductResponse createProduct(ProductRequest request) {

        if (existsProductSku(request.sku())) {

            var logRequest = new AppLoggerRequest(
                    "CREATE_PRODUCT",
                    "PRODUCT",
                    null,
                    "Falha ao criar produto: SKU já cadastrado",
                    appLogger.serializeSafely(request),
                    null,
                    "SKU já cadastrado: " + request.sku()
            );

            appLogger.logError(logRequest);

            throw new IllegalArgumentException(
                    "SKU já cadastrado: " + request.sku()
            );
        } else {

            var product = toEntity(request);
            var savedProduct = productRepository.save(product);
            var response = toResponse(savedProduct);

            var logRequest = new AppLoggerRequest(
                    "CREATE_PRODUCT",
                    "PRODUCT",
                    savedProduct.getId().toString(),
                    "Produto criado com sucesso",
                    appLogger.serializeSafely(request),
                    appLogger.serializeSafely(response),
                    null
            );

            applicationEventPublisher.publishEvent(
                    new AppLoggerSuccessEvent(logRequest)
            );

            return response;
        }
    }

    public List<ProductResponse> createAllListProduct(List<ProductRequest> requests) {

        if (requests == null || requests.isEmpty()) {

            var logRequest = new AppLoggerRequest(
                    "CREATE_ALL_PRODUCTS",
                    "PRODUCT",
                    null,
                    "Falha ao criar lista de produtos",
                    appLogger.serializeSafely(requests),
                    null,
                    "A lista de produtos não pode ser vazia"
            );

            appLogger.logError(logRequest);
            throw new IllegalArgumentException("A lista de produtos não pode ser vazia");
        }

        validateDuplicateSkus(requests);

        var skus = requests.stream().map(ProductRequest::sku).toList();
        validateExistingSkus(skus, requests);

        var products = requests.stream().map(this::toEntity).toList();
        var saveListProducts = productRepository.saveAll(products);
        var response = saveListProducts.stream().map(this::toResponse).toList();

        var logRequest = new AppLoggerRequest(
                "CREATE_ALL_PRODUCTS",
                "PRODUCT",
                null,
                "Criado lista de produtos",
                appLogger.serializeSafely(requests),
                appLogger.serializeSafely(response),
                null
        );

        applicationEventPublisher.publishEvent(
                new AppLoggerSuccessEvent(logRequest)
        );

        return response;
    }

    public ProductResponse findProductBySkuByProduct(String sku, String name) {

        if (sku != null && !sku.isBlank()) {
            var product = productRepository.findBySku(sku);
            if (product.isEmpty()) {
                var errorMessage = "Produto não encontrado com SKU informado: " + sku;
                var logRequest = new AppLoggerRequest(
                        "FIND_PRODUCT",
                        "PRODUCT",
                        null,
                        "Falha ao buscar produto por SKU",
                        appLogger.serializeSafely(sku),
                        null,
                        errorMessage
                );
                appLogger.logError(logRequest);
                throw new IllegalArgumentException(errorMessage);
            }

            var response = toResponse(product.get());
            var logRequest = new AppLoggerRequest(
                    "FIND_PRODUCT",
                    "PRODUCT",
                    product.get().getId().toString(),
                    "Produto encontrado com sucesso",
                    appLogger.serializeSafely(sku),
                    appLogger.serializeSafely(response),
                    null
            );
            applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));
            return response;
        }

        if (name != null && !name.isBlank()) {
            var product = productRepository.findProductByName(name);
            if (product.isEmpty()) {
                var errorMessage = "Produto não encontrado com o Nome informado: " + name;
                var logRequest = new AppLoggerRequest(
                        "FIND_PRODUCT",
                        "PRODUCT",
                        null,
                        "Falha ao buscar produto por Nome",
                        appLogger.serializeSafely(name),
                        null,
                        errorMessage
                );
                appLogger.logError(logRequest);
                throw new IllegalArgumentException(errorMessage);
            }

            var response = toResponse(product.get());
            var logRequest = new AppLoggerRequest(
                    "FIND_PRODUCT",
                    "PRODUCT",
                    product.get().getId().toString(),
                    "Produto encontrado com sucesso",
                    appLogger.serializeSafely(name),
                    appLogger.serializeSafely(response),
                    null
            );
            applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));
            return response;
        }

        var errorMessage = "Informe o SKU ou NOME do produto";
        var logRequest = new AppLoggerRequest(
                "FIND_PRODUCT",
                "PRODUCT",
                null,
                "Falha ao buscar produto",
                null,
                null,
                errorMessage
        );
        appLogger.logError(logRequest);
        throw new IllegalArgumentException(errorMessage);
    }

    public List<ProductResponse> getListProduct() {
        List<Product> products = productRepository.findAll();
        var response = products.stream().map(this::toResponse).collect(Collectors.toList());

        var logRequest = new AppLoggerRequest(
                "GET_LIST_PRODUCT",
                "PRODUCT",
                null,
                "Lista de produtos retornado com sucesso",
                null,
                appLogger.serializeSafely(response),
                null
        );

        applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));

        return response;
    }

    public ProductResponse updateProduct(UUID id, ProductRequest request) {

        var optionalProduct = productRepository.findById(id);
        if (optionalProduct.isEmpty()) {
            var errorMessage = "Produto não encontrado";
            var logRequest = new AppLoggerRequest(
                    "UPDATE_PRODUCT",
                    "PRODUCT",
                    id != null ? id.toString() : null,
                    "Falha ao atualizar produto: Produto não encontrado",
                    appLogger.serializeSafely(request),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new IllegalArgumentException(errorMessage);
        }

        var updateProduct = optionalProduct.get();
        var existsSkuByProduct = productRepository.existsBySku(request.sku());

        if (existsSkuByProduct && !updateProduct.getSku().equals(request.sku())) {
            var errorMessage = "SKU ja cadastrado: " + request.sku();
            var logRequest = new AppLoggerRequest(
                    "UPDATE_PRODUCT",
                    "PRODUCT",
                    id.toString(),
                    "Falha ao atualizar produto: SKU já cadastrado",
                    appLogger.serializeSafely(request),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new IllegalArgumentException(errorMessage);
        }

        updateProduct.setName(request.name());
        updateProduct.setSku(request.sku());
        updateProduct.setDescription(request.description());
        updateProduct.setPrice(request.price());

        var product = productRepository.save(updateProduct);
        var response = toResponse(product);

        var logRequest = new AppLoggerRequest(
                "UPDATE_PRODUCT",
                "PRODUCT",
                id.toString(),
                "Produto atualizado com sucesso",
                appLogger.serializeSafely(request),
                appLogger.serializeSafely(response),
                null
        );

        applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));

        return response;
    }

    public ProductResponse updateProductStatus(UUID id, ProductStatusRequest statusRequest) {

        var optionalProduct = productRepository.findById(id);
        if (optionalProduct.isEmpty()) {
            var errorMessage = "Produto não encontrado";
            var logRequest = new AppLoggerRequest(
                    "UPDATE_PRODUCT_STATUS",
                    "PRODUCT",
                    id != null ? id.toString() : null,
                    "Falha ao atualizar status do produto: Produto não encontrado",
                    appLogger.serializeSafely(statusRequest),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new IllegalArgumentException(errorMessage);
        }

        var updateProductStatus = optionalProduct.get();
        var previousStatus = updateProductStatus.getStatus();

        if (previousStatus != statusRequest.status()) {

            updateProductStatus.setStatus(statusRequest.status());
            var updateProduct = productRepository.save(updateProductStatus);
            var response = toResponse(updateProduct);

            var logRequest = new AppLoggerRequest(
                    "UPDATE_PRODUCT_STATUS",
                    "PRODUCT",
                    id.toString(),
                    "Status atualizado de " + previousStatus
                            + " para " + statusRequest.status(),
                    appLogger.serializeSafely(statusRequest),
                    appLogger.serializeSafely(response),
                    null
            );

            applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));

            return response;
        } else {

            var logRequest = new AppLoggerRequest(
                    "UPDATE_PRODUCT_STATUS",
                    "PRODUCT",
                    id.toString(),
                    "O produto já possui o status solicitado",
                    appLogger.serializeSafely(statusRequest),
                    null,
                    "O status informado é igual ao status atual"
            );

            appLogger.logError(logRequest);
            throw new IllegalArgumentException("O produto já possui o status informado");
        }
    }

    public ProductResponse removeProduct(UUID id) {

        var optionalProduct = productRepository.findById(id);
        if (optionalProduct.isEmpty()) {
            var errorMessage = "Produto inexistente ou removido da base de dados";
            var logRequest = new AppLoggerRequest(
                    "REMOVE_PRODUCT",
                    "PRODUCT",
                    id != null ? id.toString() : null,
                    "Falha ao remover produto: Produto não encontrado",
                    null,
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new IllegalArgumentException(errorMessage);
        }

        var removeProduct = optionalProduct.get();
        var response = toResponse(removeProduct);

        productRepository.delete(removeProduct);

        var logRequest = new AppLoggerRequest(
                "REMOVE_PRODUCT",
                "PRODUCT",
                id.toString(),
                "Produto removido com sucesso",
                null,
                appLogger.serializeSafely(response),
                null
        );

        applicationEventPublisher.publishEvent(new AppLoggerSuccessEvent(logRequest));

        return response;
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

    private boolean existsProductSku(String sku) {
        return productRepository.existsBySku(sku);
    }

    private void validateExistingSkus(List<String> skus, List<ProductRequest> requests) {

        var existingProducts = productRepository.findAllBySkuIn(skus);

        if (!existingProducts.isEmpty()) {
            var existingSkus = existingProducts.stream()
                    .map(Product::getSku)
                    .toList();
            var errorMessage = "Os seguintes Skus já estão cadastrados: " + existingSkus;
            var logRequest = new AppLoggerRequest(
                    "CREATE_ALL_PRODUCTS",
                    "PRODUCT",
                    null,
                    "Falha ao criar lista de produtos: SKU já cadastrado",
                    appLogger.serializeSafely(requests),
                    null,
                    errorMessage
            );
            appLogger.logError(logRequest);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void validateDuplicateSkus(List<ProductRequest> requests) {

        Set<String> skus = new HashSet<>();

        for (var request : requests) {
            if (!skus.add(request.sku())) {
                var errorMessage = "SKU duplicado na lista: " + request.sku();
                var logRequest = new AppLoggerRequest(
                        "CREATE_ALL_PRODUCTS",
                        "PRODUCT",
                        null,
                        "Falha ao criar lista de produtos: SKU duplicado na lista",
                        appLogger.serializeSafely(requests),
                        null,
                        errorMessage
                );
                appLogger.logError(logRequest);
                throw new IllegalArgumentException(errorMessage);
            }
        }
    }
}

