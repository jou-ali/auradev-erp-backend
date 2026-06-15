package com.auradev.erp.procurement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePurchaseRequest(
        @NotNull UUID supplierId,
        @NotNull LocalDate billDate,
        LocalDate dueDate,
        String notes,
        @NotEmpty @Valid List<PurchaseLineRequest> items
) {}
