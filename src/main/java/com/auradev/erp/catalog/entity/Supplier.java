package com.auradev.erp.catalog.entity;

import com.auradev.erp.common.entity.CatalogEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Shared supplier master — schema v2.0 (no tenant_id).
 */
@Getter
@Setter
@Entity
@Table(name = "suppliers")
public class Supplier extends CatalogEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "contact_person", length = 120)
    private String contactPerson;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
