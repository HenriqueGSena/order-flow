package com.backend.orderflow.repository;

import com.backend.orderflow.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository  extends JpaRepository<Product, UUID> {

    boolean existsBySku(String sku);

    List<Product> findAllBySkuIn(Collection<String> skus);
}
