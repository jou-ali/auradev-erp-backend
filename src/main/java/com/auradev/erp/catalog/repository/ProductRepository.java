package com.auradev.erp.catalog.repository;

import com.auradev.erp.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByActiveTrue(Pageable pageable);

    Optional<Product> findBySku(String sku);

    Optional<Product> findByBarcode(String barcode);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND (lower(p.name) LIKE lower(concat('%', :q, '%')) OR
                   lower(p.sku) LIKE lower(concat('%', :q, '%')) OR
                   lower(coalesce(p.barcode, '')) LIKE lower(concat('%', :q, '%')))
            """)
    Page<Product> searchActive(@Param("q") String q, Pageable pageable);
}
