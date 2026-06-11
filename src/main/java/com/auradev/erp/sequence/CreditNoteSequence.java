package com.auradev.erp.sequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Gapless credit-note-number counter per tenant per financial year.
 *
 * @see BillSequence for design notes.
 */
@Getter
@Setter
@Entity
@Table(name = "credit_note_sequences")
@IdClass(CreditNoteSequence.CreditNoteSequenceId.class)
public class CreditNoteSequence {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Id
    @Column(name = "fy", nullable = false, length = 4)
    private String fy;

    @Column(name = "last_no", nullable = false)
    private int lastNo;

    /** Composite primary key for {@link CreditNoteSequence}. */
    public static class CreditNoteSequenceId implements Serializable {
        private UUID tenantId;
        private String fy;

        public CreditNoteSequenceId() {}

        public CreditNoteSequenceId(UUID tenantId, String fy) {
            this.tenantId = tenantId;
            this.fy = fy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CreditNoteSequenceId that)) return false;
            return Objects.equals(tenantId, that.tenantId) && Objects.equals(fy, that.fy);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenantId, fy);
        }
    }
}
