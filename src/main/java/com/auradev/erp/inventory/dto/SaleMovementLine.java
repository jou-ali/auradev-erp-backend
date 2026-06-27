package com.auradev.erp.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleMovementLine(UUID productId, BigDecimal quantity) {}
