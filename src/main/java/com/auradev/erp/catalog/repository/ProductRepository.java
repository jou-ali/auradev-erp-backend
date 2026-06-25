package com.auradev.erp.catalog.repository;

import com.auradev.erp.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findWithCategoryById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByActiveTrue(Pageable pageable);

    Optional<Product> findBySku(String sku);

    @Query("SELECT p FROM Product p WHERE lower(p.sku) = lower(:sku)")
    Optional<Product> findBySkuIgnoreCase(@Param("sku") String sku);

    @Query("SELECT p FROM Product p WHERE lower(p.sku) IN :skus")
    List<Product> findBySkuInIgnoreCase(@Param("skus") Collection<String> skus);

    @Query("SELECT lower(p.sku) FROM Product p WHERE lower(p.sku) IN :skus")
    List<String> findExistingSkuKeys(@Param("skus") Collection<String> skus);

    Optional<Product> findByBarcode(String barcode);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    @EntityGraph(attributePaths = {"category"})
    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND (lower(p.name) LIKE lower(concat('%', :q, '%')) OR
                   lower(p.sku) LIKE lower(concat('%', :q, '%')) OR
                   lower(coalesce(p.barcode, '')) LIKE lower(concat('%', :q, '%')))
            """)
    Page<Product> searchActive(@Param("q") String q, Pageable pageable);
}
