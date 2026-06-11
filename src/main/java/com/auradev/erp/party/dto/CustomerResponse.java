package com.auradev.erp.party.dto;

import com.auradev.erp.party.entity.CustomerType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * API response payload representing a customer.
 *
 * @param id            surrogate UUID
 * @param name          full display name
 * @param phone         contact phone number
 * @param email         contact email address
 * @param type          customer classification
 * @param gstin         GST Identification Number (B2B customers)
 * @param stateCode     2-digit state code
 * @param address       postal/delivery address
 * @param loyaltyPoints accumulated loyalty points
 * @param creditBalance outstanding credit available
 * @param createdAt     server timestamp of record creation
 * @param updatedAt     server timestamp of last update
 */
public record CustomerResponse(
        UUID id,
        String name,
        String phone,
        String email,
        CustomerType type,
        String gstin,
        String stateCode,
        String address,
        int loyaltyPoints,
        BigDecimal creditBalance,
        Instant createdAt,
        Instant updatedAt
) {}
