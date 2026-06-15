package com.auradev.erp.inventory.repository;

import com.auradev.erp.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByTenantIdAndProductId(UUID tenantId, UUID productId);

    List<Inventory> findByTenantId(UUID tenantId);

    @Query("""
            SELECT i FROM Inventory i
            WHERE i.tenantId = :tenantId AND i.product.id IN :productIds
            """)
    List<Inventory> findByTenantIdAndProductIdIn(
            @Param("tenantId") UUID tenantId,
            @Param("productIds") Collection<UUID> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.tenantId = :tenantId AND i.product.id = :productId")
    Optional<Inventory> findForUpdate(@Param("tenantId") UUID tenantId, @Param("productId") UUID productId);

    @Modifying
    @Query("""
            UPDATE Inventory i SET i.quantityOnHand = i.quantityOnHand + :qty, i.lastUpdated = CURRENT_TIMESTAMP
            WHERE i.tenantId = :tenantId AND i.product.id = :productId
            """)
    int increment(@Param("tenantId") UUID tenantId, @Param("productId") UUID productId, @Param("qty") BigDecimal qty);

    @Modifying
    @Query("""
            UPDATE Inventory i SET i.quantityOnHand = i.quantityOnHand - :qty, i.lastUpdated = CURRENT_TIMESTAMP
            WHERE i.tenantId = :tenantId AND i.product.id = :productId AND i.quantityOnHand >= :qty
            """)
    int decrement(@Param("tenantId") UUID tenantId, @Param("productId") UUID productId, @Param("qty") BigDecimal qty);

}
