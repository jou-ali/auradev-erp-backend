package com.auradev.erp.settings.controller;

import com.auradev.erp.settings.dto.BillingSettingsResponse;
import com.auradev.erp.settings.dto.PrinterSettingsResponse;
import com.auradev.erp.settings.dto.StoreProfileResponse;
import com.auradev.erp.settings.dto.UpdateBillingSettingsRequest;
import com.auradev.erp.settings.dto.UpdatePrinterSettingsRequest;
import com.auradev.erp.settings.dto.UpdateStoreProfileRequest;
import com.auradev.erp.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/settings")
@Tag(name = "Settings", description = "Store profile and tenant configuration")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/profile")
    @PreAuthorize("@authz.canAny(authentication, 'SETTINGS_STORE_VIEW', 'SETTINGS_STORE_EDIT')")
    @Operation(summary = "Get store profile for the current tenant")
    public ResponseEntity<StoreProfileResponse> getProfile() {
        return ResponseEntity.ok(settingsService.getStoreProfile());
    }

    @PutMapping("/profile")
    @PreAuthorize("@authz.can(authentication, 'SETTINGS_STORE_EDIT')")
    @Operation(summary = "Update store profile")
    public ResponseEntity<StoreProfileResponse> updateProfile(
            @Valid @RequestBody UpdateStoreProfileRequest req) {
        return ResponseEntity.ok(settingsService.updateStoreProfile(req));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.can(authentication, 'SETTINGS_STORE_EDIT')")
    @Operation(summary = "Upload store logo (JPEG, PNG, WebP)")
    public ResponseEntity<StoreProfileResponse> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(settingsService.uploadLogo(file));
    }

    @GetMapping("/billing")
    @PreAuthorize("@authz.canAny(authentication, 'SETTINGS_BILLING_VIEW', 'SETTINGS_BILLING_EDIT', 'BILL_CREATE')")
    @Operation(summary = "Get POS billing behaviour settings")
    public ResponseEntity<BillingSettingsResponse> getBillingSettings() {
        return ResponseEntity.ok(settingsService.getBillingSettings());
    }

    @PutMapping("/billing")
    @PreAuthorize("@authz.can(authentication, 'SETTINGS_BILLING_EDIT')")
    @Operation(summary = "Update POS billing behaviour settings")
    public ResponseEntity<BillingSettingsResponse> updateBillingSettings(
            @Valid @RequestBody UpdateBillingSettingsRequest req) {
        return ResponseEntity.ok(settingsService.updateBillingSettings(req));
    }

    @GetMapping("/printer")
    @PreAuthorize("@authz.can(authentication, 'SETTINGS_PRINTER_VIEW')")
    @Operation(summary = "Get receipt printer settings for the current tenant")
    public ResponseEntity<PrinterSettingsResponse> getPrinterSettings() {
        return ResponseEntity.ok(settingsService.getPrinterSettings());
    }

    @PutMapping("/printer")
    @PreAuthorize("@authz.can(authentication, 'SETTINGS_PRINTER_EDIT')")
    @Operation(summary = "Update receipt printer settings")
    public ResponseEntity<PrinterSettingsResponse> updatePrinterSettings(
            @Valid @RequestBody UpdatePrinterSettingsRequest req) {
        return ResponseEntity.ok(settingsService.updatePrinterSettings(req));
    }
}
