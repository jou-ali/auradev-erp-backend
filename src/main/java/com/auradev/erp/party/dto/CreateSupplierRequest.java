package com.auradev.erp.party.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for creating a new supplier.
 *
 * @param name             registered/trading name (required)
 * @param categoryId       UUID of the product category (required)
 * @param gstin            GST Identification Number
 * @param stateCode        2-digit registered state code
 * @param phone            contact phone number
 * @param email            contact email address
 * @param address          registered/billing address
 * @param paymentTermsDays standard credit period in days (e.g. 30 for Net-30)
 */
public record CreateSupplierRequest(

        @NotBlank(message = "name is required")
        String name,

        @NotNull(message = "categoryId is required")
        UUID categoryId,

        String gstin,
        String stateCode,
        String phone,
        String email,
        String address,
        int paymentTermsDays
) {}
