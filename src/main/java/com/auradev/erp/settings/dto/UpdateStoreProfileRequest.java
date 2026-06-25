package com.auradev.erp.settings.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStoreProfileRequest(
        @NotBlank String name,
        String phone,
        String gstin,
        String stateCode,
        String address,
        @NotBlank String billNoPrefix,
        String billFooter
) {}
