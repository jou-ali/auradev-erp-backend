package com.auradev.erp.catalog.dto;

import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String name,
        String contactPerson,
        String phone,
        String email,
        String gstin,
        String address
) {}
