package com.auradev.erp.catalog.entity;

/**
 * Unit of measure for a product.
 *
 * <p>Stored as a STRING enum column so that new units can be added without a
 * DDL migration on the ordinal column.</p>
 */
public enum ProductUnit {
    /** Pieces — discrete, countable items. */
    PCS,
    /** Kilograms — weight-based items sold by mass. */
    KG
}
