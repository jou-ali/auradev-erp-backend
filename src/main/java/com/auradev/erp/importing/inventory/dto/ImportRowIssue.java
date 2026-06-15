package com.auradev.erp.importing.inventory.dto;

public record ImportRowIssue(
        int row,
        String sku,
        String reason
) {}
