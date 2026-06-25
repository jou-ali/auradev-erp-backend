package com.auradev.erp.settings.dto;

public record PrinterSettingsResponse(
        int widthMm,
        boolean autoPrint,
        int copies,
        boolean showLogo
) {}
