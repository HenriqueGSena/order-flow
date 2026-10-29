package com.backend.orderflow.repository;

import com.backend.orderflow.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository  extends JpaRepository<Product, UUID> {

    boolean existsBySku(String sku);

    List<Product> findAllBySkuIn(Collection<String> skus);

    Optional<Product> findBySku(String sku);

    Optional<Product> findProductByName(String name);

    @Override
    boolean existsById(UUID id);

    @Query("""
    SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
    FROM Product p
    WHERE (:sku IS NOT NULL AND p.sku = :sku)
       OR (:name IS NOT NULL AND LOWER(p.name) = LOWER(:name))
    """)
    boolean existsProductBySkuOrName(@Param("sku") String sku, @Param("name") String name);
}
