package com.auradev.erp.inventory.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps {@link MovementType#customer_return} ↔ PostgreSQL enum value {@code return}. */
@Converter(autoApply = true)
public class MovementTypeConverter implements AttributeConverter<MovementType, String> {

    @Override
    public String convertToDatabaseColumn(MovementType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.toValue();
    }

    @Override
    public MovementType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return MovementType.fromValue(dbData);
    }
}
