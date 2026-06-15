package com.auradev.erp.importing.procurement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseImportRow(
        int rowNumber,
        String purchaseRef,
        LocalDate billDate,
        LocalDate dueDate,
        String notes,
        String sku,
        BigDecimal quantity,
        BigDecimal rate
) {}
