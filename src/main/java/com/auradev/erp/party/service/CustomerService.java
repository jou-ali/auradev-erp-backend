package com.auradev.erp.party.service;

import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.party.dto.CreateCustomerRequest;
import com.auradev.erp.party.dto.CustomerResponse;
import com.auradev.erp.party.entity.Customer;
import com.auradev.erp.party.repository.CustomerRepository;
import com.auradev.erp.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain service for customer management.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * List all customers for a tenant, paginated.
     *
     * @param tenantId the tenant UUID
     * @param pageable pagination instructions
     * @return a page of customer responses
     */
    public PageResponse<CustomerResponse> list(UUID tenantId, Pageable pageable) {
        Page<Customer> page = customerRepository.findByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    /**
     * Create a new customer.
     *
     * @param req the validated request body
     * @return the saved customer as a response DTO
     */
    public CustomerResponse create(CreateCustomerRequest req) {
        UUID tenantId = TenantContext.require();

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setName(req.name());
        customer.setPhone(req.phone());
        customer.setEmail(req.email());
        customer.setType(req.type());
        customer.setGstin(req.gstin());
        customer.setStateCode(req.stateCode());
        customer.setAddress(req.address());
        customer.setLoyaltyPoints(0);
        customer.setCreditBalance(BigDecimal.ZERO);

        return toResponse(customerRepository.save(customer));
    }

    /**
     * Fetch a single customer by UUID.
     *
     * @param id the customer UUID
     * @return the customer as a response DTO
     */
    public CustomerResponse get(UUID id) {
        UUID tenantId = TenantContext.require();
        return toResponse(findForTenant(id, tenantId));
    }

    /**
     * Update mutable fields on an existing customer.
     *
     * @param id  the customer UUID
     * @param req the update payload
     * @return the updated customer as a response DTO
     */
    public CustomerResponse update(UUID id, CreateCustomerRequest req) {
        UUID tenantId = TenantContext.require();
        Customer customer = findForTenant(id, tenantId);

        customer.setName(req.name());
        customer.setPhone(req.phone());
        customer.setEmail(req.email());
        customer.setType(req.type());
        customer.setGstin(req.gstin());
        customer.setStateCode(req.stateCode());
        customer.setAddress(req.address());

        return toResponse(customerRepository.save(customer));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Customer findForTenant(UUID id, UUID tenantId) {
        return customerRepository.findById(id)
                .filter(c -> tenantId.equals(c.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getName(),
                c.getPhone(),
                c.getEmail(),
                c.getType(),
                c.getGstin(),
                c.getStateCode(),
                c.getAddress(),
                c.getLoyaltyPoints(),
                c.getCreditBalance(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
