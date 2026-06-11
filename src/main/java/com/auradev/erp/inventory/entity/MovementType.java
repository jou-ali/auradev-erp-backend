package com.auradev.erp.inventory.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Stock movement type — schema v2.0.
 * {@link #customer_return} maps to DB/API value {@code return} (Java reserved word).
 */
public enum MovementType {
    sale,
    purchase,
    adjustment_in,
    adjustment_out,
    customer_return,
    waste;

    @JsonValue
    public String toValue() {
        return this == customer_return ? "return" : name();
    }

    @JsonCreator
    public static MovementType fromValue(String value) {
        if ("return".equals(value)) {
            return customer_return;
        }
        return valueOf(value);
    }
}
