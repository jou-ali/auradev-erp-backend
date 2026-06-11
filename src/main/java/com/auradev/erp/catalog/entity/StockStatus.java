package com.auradev.erp.catalog.entity;

/**
 * Derived stock-level indicator for a {@link Product}.
 *
 * <p>Computed at read time by {@link Product#stockStatus()} — never persisted.</p>
 *
 * <ul>
 *   <li>{@link #IN}  — {@code currentStock > reorderLevel}</li>
 *   <li>{@link #LOW} — {@code 0 < currentStock <= reorderLevel}</li>
 *   <li>{@link #OUT} — {@code currentStock <= 0}</li>
 * </ul>
 */
public enum StockStatus {
    /** Adequate stock on hand. */
    IN,
    /** Stock is at or below the reorder threshold — replenishment needed. */
    LOW,
    /** No stock remaining. */
    OUT
}
