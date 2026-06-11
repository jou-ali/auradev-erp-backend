package com.auradev.erp.settings.controller;

import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.settings.entity.TenantSettings;
import com.auradev.erp.settings.repository.TenantSettingsRepository;
import com.auradev.erp.tenant.TenantContext;
import com.auradev.erp.tenant.entity.Tenant;
import com.auradev.erp.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for tenant-level settings management.
 *
 * <p>All endpoints require {@code TENANT_ADMIN} or {@code SUPER_ADMIN} role.</p>
 */
@RestController
@RequestMapping("/api/v1/settings")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class SettingsController {

    private final TenantRepository tenantRepository;
    private final TenantSettingsRepository tenantSettingsRepository;

    // -------------------------------------------------------------------------
    // Store profile
    // -------------------------------------------------------------------------

    /**
     * Return the tenant's store profile.
     *
     * @return the {@link Tenant} entity as a response
     */
    @GetMapping("/store")
    public ResponseEntity<Tenant> getStore() {
        UUID tenantId = TenantContext.require();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
        return ResponseEntity.ok(tenant);
    }

    /**
     * Update the tenant's store profile fields.
     *
     * @param update map of field name to new value (name, phone, gstin, address, etc.)
     * @return the updated {@link Tenant}
     */
    @PutMapping("/store")
    @Transactional
    public ResponseEntity<Tenant> updateStore(@RequestBody Map<String, Object> update) {
        UUID tenantId = TenantContext.require();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));

        if (update.containsKey("name"))        tenant.setName((String) update.get("name"));
        if (update.containsKey("phone"))       tenant.setPhone((String) update.get("phone"));
        if (update.containsKey("gstin"))       tenant.setGstin((String) update.get("gstin"));
        if (update.containsKey("stateCode"))   tenant.setStateCode((String) update.get("stateCode"));
        if (update.containsKey("address"))     tenant.setAddress((String) update.get("address"));
        if (update.containsKey("billNoPrefix")) tenant.setBillNoPrefix((String) update.get("billNoPrefix"));
        if (update.containsKey("billFooter"))  tenant.setBillFooter((String) update.get("billFooter"));
        if (update.containsKey("logoUrl"))     tenant.setLogoUrl((String) update.get("logoUrl"));

        return ResponseEntity.ok(tenantRepository.save(tenant));
    }

    // -------------------------------------------------------------------------
    // Tax settings
    // -------------------------------------------------------------------------

    /**
     * Return the tenant's tax settings JSON.
     *
     * @return the raw tax JSON string (or an empty map if not yet configured)
     */
    @GetMapping("/tax")
    public ResponseEntity<String> getTax() {
        UUID tenantId = TenantContext.require();
        TenantSettings settings = getOrCreate(tenantId);
        String tax = settings.getTaxJson();
        return ResponseEntity.ok(tax != null ? tax : "{}");
    }

    /**
     * Update the tenant's tax settings and evict the settings cache.
     *
     * @param taxJson new tax configuration as a raw JSON string
     * @return the updated tax JSON
     */
    @PutMapping("/tax")
    @Transactional
    @CacheEvict(value = "settings", key = "#root.methodName + ':' + T(com.auradev.erp.tenant.TenantContext).get()")
    public ResponseEntity<String> updateTax(@RequestBody String taxJson) {
        UUID tenantId = TenantContext.require();
        TenantSettings settings = getOrCreate(tenantId);
        settings.setTaxJson(taxJson);
        settings.setUpdatedAt(Instant.now());
        tenantSettingsRepository.save(settings);
        return ResponseEntity.ok(taxJson);
    }

    // -------------------------------------------------------------------------
    // Payments settings
    // -------------------------------------------------------------------------

    /**
     * Return the tenant's payment method settings JSON.
     *
     * @return the raw payments JSON string (or an empty map if not yet configured)
     */
    @GetMapping("/payments")
    public ResponseEntity<String> getPayments() {
        UUID tenantId = TenantContext.require();
        TenantSettings settings = getOrCreate(tenantId);
        String payments = settings.getPaymentsJson();
        return ResponseEntity.ok(payments != null ? payments : "{}");
    }

    /**
     * Update the tenant's payment method settings.
     *
     * @param paymentsJson new payments configuration as a raw JSON string
     * @return the updated payments JSON
     */
    @PutMapping("/payments")
    @Transactional
    public ResponseEntity<String> updatePayments(@RequestBody String paymentsJson) {
        UUID tenantId = TenantContext.require();
        TenantSettings settings = getOrCreate(tenantId);
        settings.setPaymentsJson(paymentsJson);
        settings.setUpdatedAt(Instant.now());
        tenantSettingsRepository.save(settings);
        return ResponseEntity.ok(paymentsJson);
    }

    // -------------------------------------------------------------------------
    // Printer settings
    // -------------------------------------------------------------------------

    /**
     * Return the tenant's printer / receipt settings JSON.
     *
     * @return the raw printer JSON string (or an empty map if not yet configured)
     */
    @GetMapping("/printer")
    public ResponseEntity<String> getPrinter() {
        UUID tenantId = TenantContext.require();
        TenantSettings settings = getOrCreate(tenantId);
        String printer = settings.getPrinterJson();
        return ResponseEntity.ok(printer != null ? printer : "{}");
    }

    /**
     * Update the tenant's printer / receipt settings.
     *
     * @param printerJson new printer configuration as a raw JSON string
     * @return the updated printer JSON
     */
    @PutMapping("/printer")
    @Transactional
    public ResponseEntity<String> updatePrinter(@RequestBody String printerJson) {
        UUID tenantId = TenantContext.require();
        TenantSettings settings = getOrCreate(tenantId);
        settings.setPrinterJson(printerJson);
        settings.setUpdatedAt(Instant.now());
        tenantSettingsRepository.save(settings);
        return ResponseEntity.ok(printerJson);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Fetch existing settings for a tenant or create a new empty record.
     *
     * @param tenantId the tenant UUID
     * @return existing or newly created {@link TenantSettings}
     */
    private TenantSettings getOrCreate(UUID tenantId) {
        return tenantSettingsRepository.findById(tenantId).orElseGet(() -> {
            TenantSettings s = new TenantSettings();
            s.setTenantId(tenantId);
            s.setUpdatedAt(Instant.now());
            return s;
        });
    }
}
