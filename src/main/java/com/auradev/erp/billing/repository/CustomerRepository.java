package com.auradev.erp.billing.repository;

import com.auradev.erp.billing.entity.Customer;
import com.auradev.erp.billing.entity.CustomerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByTenantIdOrderByNameAsc(UUID tenantId);

    Optional<Customer> findByTenantIdAndType(UUID tenantId, CustomerType type);
}
