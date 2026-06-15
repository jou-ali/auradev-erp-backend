package com.auradev.erp.importing.inventory;

import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.inventory.entity.MovementType;

import java.util.Locale;
import java.util.Set;

/** Same rules as Quick adjust / POST /products/{id}/stock-adjust. */
public final class StockAdjustmentMovementResolver {

    private static final Set<String> ADJUSTMENTS = Set.of("add", "remove");
    private static final Set<String> REASONS = Set.of("grn", "damage", "return", "count");

    private StockAdjustmentMovementResolver() {}

    public static MovementType resolve(String adjustment, String reason) {
        String adj = normalize(adjustment);
        String rsn = normalize(reason);
        if (!ADJUSTMENTS.contains(adj)) {
            throw new BusinessException("INVALID_ADJUSTMENT", "adjustment must be add or remove");
        }
        if (!REASONS.contains(rsn)) {
            throw new BusinessException("INVALID_REASON", "reason must be grn, damage, return, or count");
        }

        if ("add".equals(adj)) {
            return switch (rsn) {
                case "grn" -> MovementType.purchase;
                case "return" -> MovementType.customer_return;
                case "count" -> MovementType.adjustment_in;
                default -> MovementType.adjustment_in;
            };
        }
        if ("damage".equals(rsn)) {
            return MovementType.waste;
        }
        return MovementType.adjustment_out;
    }

    public static boolean isValidAdjustment(String adjustment) {
        return adjustment != null && ADJUSTMENTS.contains(normalize(adjustment));
    }

    public static boolean isValidReason(String reason) {
        return reason != null && REASONS.contains(normalize(reason));
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
