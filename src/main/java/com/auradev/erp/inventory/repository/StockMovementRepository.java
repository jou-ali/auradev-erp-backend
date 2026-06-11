package com.auradev.erp.inventory.repository;

import com.auradev.erp.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    Page<StockMovement> findByTenantIdAndProductIdOrderByCreatedAtDesc(
            UUID tenantId, UUID productId, Pageable pageable);

    @Query("SELECT COUNT(m) > 0 FROM StockMovement m WHERE m.product.id = :productId")
    boolean existsByProductId(@Param("productId") UUID productId);
}
