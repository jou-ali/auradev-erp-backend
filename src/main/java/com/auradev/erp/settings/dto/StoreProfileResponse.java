package com.auradev.erp.settings.dto;

import java.util.UUID;

public record StoreProfileResponse(
        UUID tenantId,
        String name,
        String phone,
        String gstin,
        String stateCode,
        String address,
        String billNoPrefix,
        String billFooter,
        String logoUrl
) {}
