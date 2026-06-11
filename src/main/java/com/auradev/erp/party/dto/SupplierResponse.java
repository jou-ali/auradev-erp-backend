package com.auradev.erp.party.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * API response payload representing a supplier.
 *
 * @param id               surrogate UUID
 * @param name             registered/trading name
 * @param categoryId       UUID of the associated product category
 * @param categoryName     display name of the associated category
 * @param gstin            GST Identification Number
 * @param stateCode        2-digit registered state code
 * @param phone            contact phone number
 * @param email            contact email address
 * @param address          registered/billing address
 * @param paymentTermsDays standard credit period in days
 * @param createdAt        server timestamp of record creation
 * @param updatedAt        server timestamp of last update
 */
public record SupplierResponse(
        UUID id,
        String name,
        UUID categoryId,
        String categoryName,
        String gstin,
        String stateCode,
        String phone,
        String email,
        String address,
        int paymentTermsDays,
        Instant createdAt,
        Instant updatedAt
) {}
