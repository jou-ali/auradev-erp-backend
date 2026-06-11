package com.auradev.erp.catalog.entity;

import com.auradev.erp.common.entity.CatalogEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Shared product category — schema v2.0 (no tenant_id).
 */
@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends CatalogEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
