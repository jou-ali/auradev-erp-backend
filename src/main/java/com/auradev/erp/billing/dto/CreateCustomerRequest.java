package com.auradev.erp.billing.dto;

import com.auradev.erp.billing.entity.CustomerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 20) String phone,
        CustomerType type,
        @Size(max = 15) String gstin
) {}
