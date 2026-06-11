package com.auradev.erp.party.service;

import com.auradev.erp.catalog.entity.Category;
import com.auradev.erp.catalog.repository.CategoryRepository;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.party.dto.CreateSupplierRequest;
import com.auradev.erp.party.dto.SupplierResponse;
import com.auradev.erp.party.entity.Supplier;
import com.auradev.erp.party.repository.SupplierRepository;
import com.auradev.erp.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Domain service for supplier management.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;

    /**
     * List all suppliers for a tenant, paginated.
     *
     * @param tenantId the tenant UUID
     * @param pageable pagination instructions
     * @return a page of supplier responses
     */
    public PageResponse<SupplierResponse> list(UUID tenantId, Pageable pageable) {
        Page<Supplier> page = supplierRepository.findByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    /**
     * Create a new supplier.
     *
     * @param req the validated request body
     * @return the saved supplier as a response DTO
     */
    public SupplierResponse create(CreateSupplierRequest req) {
        UUID tenantId = TenantContext.require();

        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + req.categoryId()));

        Supplier supplier = new Supplier();
        supplier.setTenantId(tenantId);
        supplier.setName(req.name());
        supplier.setCategory(category);
        supplier.setGstin(req.gstin());
        supplier.setStateCode(req.stateCode());
        supplier.setPhone(req.phone());
        supplier.setEmail(req.email());
        supplier.setAddress(req.address());
        supplier.setPaymentTermsDays(req.paymentTermsDays());

        return toResponse(supplierRepository.save(supplier));
    }

    /**
     * Fetch a single supplier by UUID.
     *
     * @param id the supplier UUID
     * @return the supplier as a response DTO
     */
    public SupplierResponse get(UUID id) {
        UUID tenantId = TenantContext.require();
        return toResponse(findForTenant(id, tenantId));
    }

    /**
     * Update mutable fields on an existing supplier.
     *
     * @param id  the supplier UUID
     * @param req the update payload
     * @return the updated supplier as a response DTO
     */
    public SupplierResponse update(UUID id, CreateSupplierRequest req) {
        UUID tenantId = TenantContext.require();
        Supplier supplier = findForTenant(id, tenantId);

        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + req.categoryId()));

        supplier.setName(req.name());
        supplier.setCategory(category);
        supplier.setGstin(req.gstin());
        supplier.setStateCode(req.stateCode());
        supplier.setPhone(req.phone());
        supplier.setEmail(req.email());
        supplier.setAddress(req.address());
        supplier.setPaymentTermsDays(req.paymentTermsDays());

        return toResponse(supplierRepository.save(supplier));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Supplier findForTenant(UUID id, UUID tenantId) {
        return supplierRepository.findById(id)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
    }

    private SupplierResponse toResponse(Supplier s) {
        UUID categoryId = s.getCategory() != null ? s.getCategory().getId() : null;
        String categoryName = s.getCategory() != null ? s.getCategory().getName() : null;
        return new SupplierResponse(
                s.getId(),
                s.getName(),
                categoryId,
                categoryName,
                s.getGstin(),
                s.getStateCode(),
                s.getPhone(),
                s.getEmail(),
                s.getAddress(),
                s.getPaymentTermsDays(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
