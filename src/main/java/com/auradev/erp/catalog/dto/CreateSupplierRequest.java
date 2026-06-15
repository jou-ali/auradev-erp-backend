package com.auradev.erp.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSupplierRequest(
        @NotBlank String name,
        String contactPerson,
        String phone,
        String email,
        String gstin,
        String address
) {}
