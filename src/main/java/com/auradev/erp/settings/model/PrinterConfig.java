package com.auradev.erp.settings.model;

/**
 * Receipt printer preferences stored in {@code tenant_settings.printer} JSONB.
 */
public record PrinterConfig(
        int widthMm,
        boolean autoPrint,
        int copies,
        boolean showLogo
) {
    public static PrinterConfig defaults() {
        return new PrinterConfig(80, false, 1, true);
    }

    public PrinterConfig normalized() {
        int w = widthMm == 58 ? 58 : 80;
        int c = copies < 1 ? 1 : Math.min(copies, 3);
        return new PrinterConfig(w, autoPrint, c, showLogo);
    }
}
