package com.auradev.erp.party.dto;

import com.auradev.erp.party.entity.CustomerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating or importing a customer.
 *
 * @param name      full display name (required)
 * @param phone     contact phone number (required)
 * @param type      customer classification (required)
 * @param gstin     GST Identification Number (optional; relevant for B2B)
 * @param stateCode 2-digit state code (optional)
 * @param address   postal/delivery address (optional)
 * @param email     contact email address (optional)
 */
public record CreateCustomerRequest(

        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "phone is required")
        String phone,

        @NotNull(message = "type is required")
        CustomerType type,

        String gstin,
        String stateCode,
        String address,
        String email
) {}
