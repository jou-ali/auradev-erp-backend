package com.auradev.erp.search.dto;

public record SearchHitDto(
        String type,
        String id,
        String label,
        String subtitle,
        String query
) {}
