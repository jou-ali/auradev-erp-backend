package com.auradev.erp.settings.service;

import com.auradev.erp.audit.service.AuditService;
import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.settings.dto.BillingSettingsResponse;
import com.auradev.erp.settings.dto.PrinterSettingsResponse;
import com.auradev.erp.settings.dto.StoreProfileResponse;
import com.auradev.erp.settings.dto.TaxSettingsResponse;
import com.auradev.erp.settings.dto.UpdateBillingSettingsRequest;
import com.auradev.erp.settings.dto.UpdatePrinterSettingsRequest;
import com.auradev.erp.settings.dto.UpdateStoreProfileRequest;
import com.auradev.erp.settings.dto.UpdateTaxSettingsRequest;
import com.auradev.erp.settings.entity.TenantSettings;
import com.auradev.erp.settings.model.BillingConfig;
import com.auradev.erp.settings.model.CategoryGstRate;
import com.auradev.erp.settings.model.PrinterConfig;
import com.auradev.erp.settings.model.TaxConfig;
import com.auradev.erp.settings.repository.TenantSettingsRepository;
import com.auradev.erp.tenant.TenantContext;
import com.auradev.erp.tenant.entity.Tenant;
import com.auradev.erp.tenant.repository.TenantRepository;
import com.auradev.erp.catalog.entity.Category;
import com.auradev.erp.catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SettingsService {

    private static final Set<String> ALLOWED_LOGO_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/jpg");
    private static final long MAX_LOGO_BYTES = 2 * 1024 * 1024;

    private final TenantRepository tenantRepository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final CategoryRepository categoryRepository;
    private final AuditService auditService;

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    @Transactional(readOnly = true)
    public StoreProfileResponse getStoreProfile() {
        return toResponse(loadTenant());
    }

    public StoreProfileResponse updateStoreProfile(UpdateStoreProfileRequest req) {
        Tenant tenant = loadTenant();
        tenant.setName(req.name().trim());
        tenant.setPhone(blankToNull(req.phone()));
        tenant.setGstin(blankToNull(req.gstin()));
        if (req.stateCode() != null && !req.stateCode().isBlank()) {
            tenant.setStateCode(req.stateCode().trim());
        }
        tenant.setAddress(blankToNull(req.address()));
        tenant.setBillNoPrefix(req.billNoPrefix().trim());
        tenant.setBillFooter(blankToNull(req.billFooter()));
        Tenant saved = tenantRepository.save(tenant);
        auditService.log("STORE_PROFILE_UPDATED", "tenant", saved.getId(), Map.of("name", saved.getName()));
        return toResponse(saved);
    }

    public StoreProfileResponse uploadLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Choose a logo image to upload");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new BusinessException("FILE_TOO_LARGE", "Logo must be 2 MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_LOGO_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("INVALID_FILE", "Logo must be JPEG, PNG, or WebP");
        }

        Tenant tenant = loadTenant();
        String ext = extensionFor(contentType);
        Path dir = Path.of(uploadsDir, "tenants", tenant.getId().toString());
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve("logo." + ext);
            Files.write(target, file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("UPLOAD_FAILED", "Could not save logo: " + e.getMessage());
        }

        String logoPath = "/uploads/tenants/" + tenant.getId() + "/logo." + ext;
        tenant.setLogoUrl(publicBaseUrl + logoPath);
        Tenant saved = tenantRepository.save(tenant);
        auditService.log("LOGO_UPLOADED", "tenant", saved.getId(), null);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PrinterSettingsResponse getPrinterSettings() {
        return toPrinterResponse(loadOrCreateSettings().getPrinter());
    }

    public PrinterSettingsResponse updatePrinterSettings(UpdatePrinterSettingsRequest req) {
        TenantSettings settings = loadOrCreateSettings();
        PrinterConfig current = settings.getPrinter() != null
                ? settings.getPrinter().normalized()
                : PrinterConfig.defaults();

        int widthMm = req.widthMm() != null ? req.widthMm() : current.widthMm();
        boolean autoPrint = req.autoPrint() != null ? req.autoPrint() : current.autoPrint();
        int copies = req.copies() != null ? req.copies() : current.copies();
        boolean showLogo = req.showLogo() != null ? req.showLogo() : current.showLogo();

        PrinterConfig updated = new PrinterConfig(widthMm, autoPrint, copies, showLogo).normalized();
        settings.setPrinter(updated);
        settings.setUpdatedBy(currentUserId());
        tenantSettingsRepository.save(settings);

        auditService.log("PRINTER_SETTINGS_UPDATED", "tenant_settings", settings.getTenantId(), Map.of(
                "widthMm", updated.widthMm(),
                "autoPrint", updated.autoPrint(),
                "copies", updated.copies(),
                "showLogo", updated.showLogo()));
        return toPrinterResponse(updated);
    }

    @Transactional(readOnly = true)
    public BillingSettingsResponse getBillingSettings() {
        return toBillingResponse(loadOrCreateSettings().getBilling());
    }

    @Transactional(readOnly = true)
    public BillingConfig getBillingConfig() {
        BillingConfig billing = loadOrCreateSettings().getBilling();
        return billing != null ? billing.normalized() : BillingConfig.defaults();
    }

    public BillingSettingsResponse updateBillingSettings(UpdateBillingSettingsRequest req) {
        TenantSettings settings = loadOrCreateSettings();
        BillingConfig current = settings.getBilling() != null
                ? settings.getBilling().normalized()
                : BillingConfig.defaults();

        BillingConfig updated = new BillingConfig(
                req.maxLineDiscountPercent() != null ? req.maxLineDiscountPercent() : current.maxLineDiscountPercent(),
                req.maxBillDiscountPercent() != null ? req.maxBillDiscountPercent() : current.maxBillDiscountPercent(),
                req.cashierMaxBillDiscountPercent() != null ? req.cashierMaxBillDiscountPercent() : current.cashierMaxBillDiscountPercent(),
                req.allowHoldBill() != null ? req.allowHoldBill() : current.allowHoldBill(),
                req.allowCreditSales() != null ? req.allowCreditSales() : current.allowCreditSales(),
                req.showCashierOnReceipt() != null ? req.showCashierOnReceipt() : current.showCashierOnReceipt(),
                req.showGstBreakupOnReceipt() != null ? req.showGstBreakupOnReceipt() : current.showGstBreakupOnReceipt(),
                req.showCustomerOnReceipt() != null ? req.showCustomerOnReceipt() : current.showCustomerOnReceipt(),
                req.roundTotalToRupee() != null ? req.roundTotalToRupee() : current.roundTotalToRupee()
        ).normalized();

        settings.setBilling(updated);
        settings.setUpdatedBy(currentUserId());
        tenantSettingsRepository.save(settings);

        auditService.log("BILLING_SETTINGS_UPDATED", "tenant_settings", settings.getTenantId(), Map.of(
                "maxBillDiscountPercent", updated.maxBillDiscountPercent(),
                "allowHoldBill", updated.allowHoldBill(),
                "allowCreditSales", updated.allowCreditSales()));
        return toBillingResponse(updated);
    }

    @Transactional(readOnly = true)
    public TaxSettingsResponse getTaxSettings() {
        return toTaxResponse(loadOrCreateSettings().getTax());
    }

    @Transactional(readOnly = true)
    public TaxConfig getTaxConfig() {
        TaxConfig tax = loadOrCreateSettings().getTax();
        return tax != null ? tax.normalized() : TaxConfig.defaults();
    }

    public TaxSettingsResponse updateTaxSettings(UpdateTaxSettingsRequest req) {
        TenantSettings settings = loadOrCreateSettings();
        TaxConfig current = settings.getTax() != null
                ? settings.getTax().normalized()
                : TaxConfig.defaults();

        List<CategoryGstRate> categoryRates = current.categoryRates();
        if (req.categoryRates() != null) {
            categoryRates = req.categoryRates().stream()
                    .map(r -> {
                        Category cat = categoryRepository.findById(r.categoryId())
                                .orElseThrow(() -> new BusinessException(
                                        "UNKNOWN_CATEGORY",
                                        "Unknown category: " + r.categoryId()));
                        return new CategoryGstRate(cat.getId(), r.ratePct());
                    })
                    .toList();
        }

        TaxConfig updated = new TaxConfig(
                req.scheme() != null ? req.scheme() : current.scheme(),
                req.priceIncludesTax() != null ? req.priceIncludesTax() : current.priceIncludesTax(),
                req.enabledRates() != null ? req.enabledRates() : current.enabledRates(),
                req.compositeRatePct() != null ? req.compositeRatePct() : current.compositeRatePct(),
                req.defaultCategoryRatePct() != null ? req.defaultCategoryRatePct() : current.defaultCategoryRatePct(),
                categoryRates
        ).normalized();

        settings.setTax(updated);
        settings.setUpdatedBy(currentUserId());
        tenantSettingsRepository.save(settings);

        auditService.log("TAX_SETTINGS_UPDATED", "tenant_settings", settings.getTenantId(), Map.of(
                "scheme", updated.scheme().name(),
                "compositeRatePct", updated.compositeRatePct(),
                "categoryMappings", updated.categoryRates().size()));
        return toTaxResponse(updated);
    }

    private TenantSettings loadOrCreateSettings() {
        UUID tenantId = TenantContext.require();
        return tenantSettingsRepository.findById(tenantId).orElseGet(() -> {
            TenantSettings created = new TenantSettings();
            created.setTenantId(tenantId);
            created.setPrinter(PrinterConfig.defaults());
            created.setBilling(BillingConfig.defaults());
            created.setTax(TaxConfig.defaults());
            created.setUpdatedBy(currentUserId());
            return tenantSettingsRepository.save(created);
        });
    }

    private static PrinterSettingsResponse toPrinterResponse(PrinterConfig config) {
        PrinterConfig c = config.normalized();
        return new PrinterSettingsResponse(c.widthMm(), c.autoPrint(), c.copies(), c.showLogo());
    }

    private static BillingSettingsResponse toBillingResponse(BillingConfig config) {
        BillingConfig c = config.normalized();
        return new BillingSettingsResponse(
                c.maxLineDiscountPercent(),
                c.maxBillDiscountPercent(),
                c.cashierMaxBillDiscountPercent(),
                c.allowHoldBill(),
                c.allowCreditSales(),
                c.showCashierOnReceipt(),
                c.showGstBreakupOnReceipt(),
                c.showCustomerOnReceipt(),
                c.roundTotalToRupee());
    }

    private TaxSettingsResponse toTaxResponse(TaxConfig config) {
        TaxConfig c = config != null ? config.normalized() : TaxConfig.defaults();
        List<TaxSettingsResponse.CategoryGstRateResponse> rows = c.categoryRates().stream()
                .map(row -> {
                    String name = categoryRepository.findById(row.categoryId())
                            .map(Category::getName)
                            .orElse("Unknown");
                    return new TaxSettingsResponse.CategoryGstRateResponse(
                            row.categoryId(), name, row.ratePct());
                })
                .toList();
        return new TaxSettingsResponse(
                c.scheme(),
                c.priceIncludesTax(),
                c.enabledRates(),
                c.compositeRatePct(),
                c.defaultCategoryRatePct(),
                rows);
    }

    private static UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private Tenant loadTenant() {
        UUID tenantId = TenantContext.require();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));
    }

    private StoreProfileResponse toResponse(Tenant tenant) {
        return new StoreProfileResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getPhone(),
                tenant.getGstin(),
                tenant.getStateCode(),
                tenant.getAddress(),
                tenant.getBillNoPrefix(),
                tenant.getBillFooter(),
                tenant.getLogoUrl());
    }

    private static String blankToNull(String v) {
        if (v == null || v.isBlank()) return null;
        return v.trim();
    }

    private static String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
