package com.auradev.erp.sequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Gapless bill-number counter per tenant per financial year.
 *
 * <p>Rows are locked with {@code SELECT ... FOR UPDATE} before incrementing so
 * that concurrent transactions cannot read the same counter value.</p>
 *
 * <p>Composite PK: (tenantId, fy).</p>
 */
@Getter
@Setter
@Entity
@Table(name = "bill_sequences")
@IdClass(BillSequence.BillSequenceId.class)
public class BillSequence {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Financial year string in {@code YYYY} format, where YYYY is the start year
     * of the FY.  E.g. for FY 2025-26 this value is {@code "2526"}.
     */
    @Id
    @Column(name = "fy", nullable = false, length = 4)
    private String fy;

    /** The last sequence number that was allocated; 0 means no bills yet this FY. */
    @Column(name = "last_no", nullable = false)
    private int lastNo;

    // -------------------------------------------------------------------------
    // Composite PK class
    // -------------------------------------------------------------------------

    /** Composite primary key for {@link BillSequence}. */
    public static class BillSequenceId implements Serializable {
        private UUID tenantId;
        private String fy;

        public BillSequenceId() {}

        public BillSequenceId(UUID tenantId, String fy) {
            this.tenantId = tenantId;
            this.fy = fy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BillSequenceId that)) return false;
            return java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(fy, that.fy);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, fy);
        }
    }
}
