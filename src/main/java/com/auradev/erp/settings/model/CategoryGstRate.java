package com.auradev.erp.settings.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryGstRate(UUID categoryId, BigDecimal ratePct) {}
