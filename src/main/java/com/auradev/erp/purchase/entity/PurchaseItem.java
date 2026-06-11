package com.auradev.erp.purchase.entity;

import com.auradev.erp.catalog.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single line item on a {@link Purchase}.
 *
 * <p>Amounts are stored denormalised so that historical totals remain stable
 * even when the catalogue price or GST rate changes later.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "purchase_items")
public class PurchaseItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The parent purchase this line belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    /** Catalogue product being purchased. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Number of units purchased (supports fractional / weight-based products). */
    @Column(name = "quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    /** Per-unit cost excluding tax, as agreed with the supplier. */
    @Column(name = "rate", nullable = false, precision = 19, scale = 2)
    private BigDecimal rate;

    /** GST percentage applicable to this line, e.g. {@code 18.00} for 18 %. */
    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRate;

    /** {@code quantity × rate} — taxable value for this line. */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** GST amount for this line: {@code amount × gstRate / 100}. */
    @Column(name = "gst_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal gstAmount;
}
