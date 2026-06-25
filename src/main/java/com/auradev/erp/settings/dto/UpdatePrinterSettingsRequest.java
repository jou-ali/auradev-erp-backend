package com.auradev.erp.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdatePrinterSettingsRequest(
        @Min(58) @Max(80) Integer widthMm,
        Boolean autoPrint,
        @Min(1) @Max(3) Integer copies,
        Boolean showLogo
) {}
