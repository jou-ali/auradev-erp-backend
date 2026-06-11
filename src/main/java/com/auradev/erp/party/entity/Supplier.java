package com.auradev.erp.party.entity;

import com.auradev.erp.catalog.entity.Category;
import com.auradev.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.util.UUID;

/**
 * A supplier from whom the tenant sources products.
 *
 * <p>The {@code category} association allows grouping suppliers by the type
 * of goods they supply (e.g. electronics, FMCG).</p>
 */
@Getter
@Setter
@Entity
@Table(name = "suppliers")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Supplier extends BaseEntity {

    /** Registered / trading name of the supplier. */
    @Column(name = "name", nullable = false)
    private String name;

    /** Category of goods typically sourced from this supplier. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /** GST Identification Number of the supplier. */
    @Column(name = "gstin", length = 15)
    private String gstin;

    /** 2-digit state code of the supplier's registered state. */
    @Column(name = "state_code", length = 2)
    private String stateCode;

    /** Primary contact phone number. */
    @Column(name = "phone")
    private String phone;

    /** Contact email address. */
    @Column(name = "email")
    private String email;

    /** Registered / billing address. */
    @Column(name = "address", columnDefinition = "text")
    private String address;

    /**
     * Standard credit period in days agreed with this supplier
     * (e.g. {@code 30} = Net 30).
     */
    @Column(name = "payment_terms_days", nullable = false)
    private int paymentTermsDays;
}
