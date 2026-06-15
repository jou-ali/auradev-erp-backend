package com.auradev.erp.catalog.repository;

import com.auradev.erp.catalog.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    List<Supplier> findByActiveTrueOrderByNameAsc();

    Optional<Supplier> findByNameIgnoreCaseAndActiveTrue(String name);

    Optional<Supplier> findByGstinIgnoreCaseAndActiveTrue(String gstin);

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);
}
