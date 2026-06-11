package com.auradev.erp.catalog.entity;

import com.auradev.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.util.UUID;

/**
 * Product category used to organise the catalog.
 *
 * <p>The Hibernate tenant filter ({@code tenantFilter}) must be enabled
 * by the repository/service layer before querying so that all reads are
 * automatically scoped to the current tenant.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "categories")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Category extends BaseEntity {

    /** Display name of the category (e.g. "Beverages", "Dairy"). */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Position in the category list; lower values appear first.
     * Defaults to 0 (first position).
     */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Whether the category is visible / selectable in the catalog. */
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
