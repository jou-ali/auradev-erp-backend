package com.auradev.erp.party.repository;

import com.auradev.erp.party.entity.Customer;
import com.auradev.erp.party.entity.CustomerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Customer} entities.
 */
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Return all customers for a tenant, paginated.
     *
     * @param tenantId the tenant UUID
     * @param pageable pagination and sort instructions
     * @return a page of customers
     */
    Page<Customer> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Look up a customer by their phone number within a tenant.
     *
     * @param tenantId the tenant UUID
     * @param phone    the customer's phone number
     * @return the matching customer, if any
     */
    Optional<Customer> findByTenantIdAndPhone(UUID tenantId, String phone);

    /**
     * Return customers of a specific type for a tenant, paginated.
     *
     * @param tenantId the tenant UUID
     * @param type     the customer classification
     * @param pageable pagination and sort instructions
     * @return a page of matching customers
     */
    Page<Customer> findByTenantIdAndType(UUID tenantId, CustomerType type, Pageable pageable);
}
