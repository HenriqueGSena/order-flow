package com.backend.orderflow.repository;

import com.backend.orderflow.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository  extends JpaRepository<Product, Long> {
    boolean existsBySku(String sku);
}
